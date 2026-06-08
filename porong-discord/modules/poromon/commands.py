"""
포로몬 명령어 모듈.

포로몬(모드 서버) 전용 명령어. RPG 모듈과 완전히 분리한다.
실제 포로몬 서버 연동은 integrations/poromon_api.py 를 통해서만 한다.

현재 구현:
  - /인증코드 <code>  : 포로몬 인증 코드 검증 → 인증 역할 부여
  - DM 핸들러         : 봇에게 코드만 DM 해도 동일하게 인증 처리
    (DM 메시지는 message_content 특권 인텐트 없이도 content 가 전달됨)

TODO (설계 확정 후):
  - /포로몬현황  : 모드 서버 상태(접속 인원·TPS) 조회
  - /포로몬도감  : 포로몬 도감/보유 현황 조회
  - 포로몬 이벤트 알림 (포로몬알림 역할 멘션)
"""
from __future__ import annotations

import logging

import discord
from discord import app_commands
from discord.ext import commands

from core import config
from integrations.poromon_api import PoromonApiClient, PoromonAuthError

log = logging.getLogger(__name__)

# 사용자 안내 문구
_MSG_SUCCESS = "✅ 인증이 완료되었습니다! 포로몬 서버 접속이 가능합니다."
_MSG_NOT_FOUND = "코드가 올바르지 않거나 만료되었습니다. 코드를 다시 확인해주세요."
_MSG_ERROR = "인증 처리 중 오류가 발생했습니다. 잠시 후 다시 시도하거나 운영진에게 문의해주세요."
_MSG_EMPTY = "인증 코드를 입력해주세요. (예: `/인증코드 ABC123`)"


class PoromonCog(commands.Cog):
    """포로몬 도메인 Cog."""

    def __init__(self, bot: commands.Bot) -> None:
        self.bot = bot
        self.api = PoromonApiClient()

    async def cog_unload(self) -> None:
        await self.api.close()

    # ─── 인증 처리 (슬래시/DM 공용) ──────────────────────────────────

    async def _process_verification(
        self,
        user: discord.abc.User,
        guild: discord.Guild | None,
        code: str,
    ) -> str:
        """코드 검증 → (성공 시) 역할 부여. 사용자에게 보여줄 문구를 반환한다."""
        code = (code or "").strip()
        if not code:
            return _MSG_EMPTY

        try:
            result = await self.api.verify_code(code, user.id)
        except PoromonAuthError as e:
            # 401/네트워크/미설정 — 운영자 확인 필요. 사용자에겐 일반 오류 안내.
            log.error("포로몬 인증 API 오류 (discord=%s): %s", user.id, e)
            return _MSG_ERROR

        if not result.get("ok"):
            return _MSG_NOT_FOUND

        # 성공 — 인증 역할 부여 + (선택) MC UUID 기록
        await self._grant_verified_role(guild, user)
        uuid = result.get("uuid")
        if uuid:
            log.info("포로몬 인증 완료: discord=%s mc_uuid=%s", user.id, uuid)
        else:
            log.info("포로몬 인증 완료: discord=%s (uuid 없음)", user.id)
        return _MSG_SUCCESS

    async def _grant_verified_role(
        self, guild: discord.Guild | None, user: discord.abc.User
    ) -> None:
        """인증유저 역할 부여 + (있으면) 미인증 역할 제거."""
        if guild is None:
            log.warning("인증 역할 부여 불가 — 길드 컨텍스트 없음 (discord=%s)", user.id)
            return

        member = guild.get_member(user.id)
        if member is None:
            try:
                member = await guild.fetch_member(user.id)
            except discord.HTTPException:
                log.warning("인증 역할 부여 불가 — 길드 멤버 아님 (discord=%s)", user.id)
                return

        verified = guild.get_role(config.ROLE_인증유저_ID) if config.ROLE_인증유저_ID else None
        unverified = guild.get_role(config.ROLE_미인증_ID) if config.ROLE_미인증_ID else None
        try:
            if verified and verified not in member.roles:
                await member.add_roles(verified, reason="포로몬 인증 코드 확인")
            if unverified and unverified in member.roles:
                await member.remove_roles(unverified, reason="포로몬 인증 완료 — 미인증 해제")
        except discord.Forbidden:
            log.warning("역할 부여 권한 없음 — 봇 권한 확인 필요 (discord=%s)", user.id)

    # ─── /인증코드 슬래시 명령 ────────────────────────────────────────

    @app_commands.command(
        name="인증코드", description="포로몬 서버 인증 코드를 입력해 인증을 완료합니다."
    )
    @app_commands.describe(code="마인크래프트에서 발급받은 인증 코드")
    async def verify_code_cmd(self, interaction: discord.Interaction, code: str) -> None:
        await interaction.response.defer(ephemeral=True)
        guild = interaction.guild or self.bot.get_guild(config.GUILD_ID)
        msg = await self._process_verification(interaction.user, guild, code)
        await interaction.followup.send(msg, ephemeral=True)

    # ─── DM 핸들러 (코드만 DM 해도 인증) ──────────────────────────────

    @commands.Cog.listener()
    async def on_message(self, message: discord.Message) -> None:
        if message.author.bot:
            return
        if message.guild is not None:
            return  # DM 만 처리 (길드 메시지는 슬래시 명령으로)

        content = (message.content or "").strip()
        if not content:
            return

        # 코드 형태(공백 없는 단일 토큰)만 인증 시도. 일반 대화는 안내로 폴백.
        tokens = content.split()
        if len(tokens) != 1 or len(tokens[0]) > 32:
            await message.channel.send(
                "포로몬 인증은 코드만 입력하거나, 서버에서 `/인증코드 <코드>` 를 사용해주세요."
            )
            return

        guild = self.bot.get_guild(config.GUILD_ID)
        msg = await self._process_verification(message.author, guild, tokens[0])
        await message.channel.send(msg)


async def setup(bot: commands.Bot) -> None:
    await bot.add_cog(PoromonCog(bot))
