"""
디스코드 약관 동의 버튼 → 닉네임 입력 모달 → 인증 코드 발급 흐름.
"""
from __future__ import annotations

import discord
from discord.ext import commands

import config
from api_client import EmpireApiClient


# ─── 닉네임 입력 모달 ────────────────────────────────────────────

class NicknameModal(discord.ui.Modal, title="마인크래프트 닉네임 입력"):
    nickname = discord.ui.TextInput(
        label="마인크래프트 닉네임",
        placeholder="닉네임 입력",
        min_length=2,
        max_length=16,
    )

    def __init__(self, api: EmpireApiClient) -> None:
        super().__init__()
        self.api = api

    async def on_submit(self, interaction: discord.Interaction) -> None:
        nick = self.nickname.value.strip()
        if not nick:
            await interaction.response.send_message("닉네임을 입력해주세요.", ephemeral=True)
            return

        await interaction.response.defer(ephemeral=True)

        try:
            data = await self.api.create_pending(str(interaction.user.id), nick)
        except Exception as e:
            await interaction.followup.send(
                f"서버 연결 오류: {e}\n잠시 후 다시 시도해주세요.", ephemeral=True
            )
            return

        code: str = data["code"]
        expires_in: int = data.get("expires_in", 600)

        # "접속대기" 역할 부여
        guild = interaction.guild
        if guild:
            role = guild.get_role(config.ROLE_접속대기_ID)
            if role:
                try:
                    await interaction.user.add_roles(role, reason="디스코드 인증 - 접속 대기")
                except discord.Forbidden:
                    pass  # 권한 없음 — 운영자가 봇 권한 확인 필요

        msg = (
            f"인증 코드: **{code}**\n"
            f"마인크래프트에서 `/연동 {code}` 를 입력해주세요. ({expires_in // 60}분 내 입력)"
        )
        await interaction.followup.send(msg, ephemeral=True)

        # DM으로도 발송 시도
        try:
            await interaction.user.send(msg)
        except (discord.Forbidden, discord.HTTPException):
            pass  # DM 차단 — 무시


# ─── 약관 동의 버튼 뷰 ───────────────────────────────────────────

class TermsView(discord.ui.View):
    def __init__(self, api: EmpireApiClient) -> None:
        super().__init__(timeout=None)  # 영구 뷰
        self.api = api

    @discord.ui.button(
        label="약관 동의",
        style=discord.ButtonStyle.green,
        custom_id="terms_agree",
    )
    async def terms_agree(self, interaction: discord.Interaction, button: discord.ui.Button) -> None:
        await interaction.response.send_modal(NicknameModal(self.api))


# ─── Auth Cog ────────────────────────────────────────────────────

class AuthCog(commands.Cog):
    def __init__(self, bot: commands.Bot) -> None:
        self.bot = bot
        self.api = EmpireApiClient()

    @commands.Cog.listener()
    async def on_ready(self) -> None:
        await self._setup_terms_message()

    @commands.Cog.listener()
    async def on_member_join(self, member: discord.Member) -> None:
        if member.guild.id != config.GUILD_ID:
            return
        if config.ROLE_미인증_ID == 0:
            return
        role = member.guild.get_role(config.ROLE_미인증_ID)
        if role:
            try:
                await member.add_roles(role, reason="신규 입장 — 미인증")
            except discord.Forbidden:
                pass

    async def _setup_terms_message(self) -> None:
        """인증 채널에 약관 메시지를 게시(없으면 새로 생성, 있으면 유지)."""
        guild = self.bot.get_guild(config.GUILD_ID)
        if guild is None:
            return
        channel = guild.get_channel(config.CHANNEL_AUTH_ID)
        if not isinstance(channel, discord.TextChannel):
            return

        view = TermsView(self.api)
        self.bot.add_view(view)  # 영구 뷰 등록

        # 이미 등록된 메시지 ID가 있으면 재사용
        if config.TERMS_MESSAGE_ID:
            try:
                await channel.fetch_message(config.TERMS_MESSAGE_ID)
                return  # 이미 존재
            except (discord.NotFound, discord.HTTPException):
                pass

        embed = discord.Embed(
            title="포로 서버 입장 인증",
            description=(
                "아래 **약관 동의** 버튼을 클릭하고 마인크래프트 닉네임을 입력하면\n"
                "인증 코드가 발급됩니다.\n\n"
                "마인크래프트에서 `/연동 <코드>` 를 입력하면 인증이 완료됩니다."
            ),
            color=discord.Color.green(),
        )
        await channel.send(embed=embed, view=view)

    async def cog_unload(self) -> None:
        await self.api.close()


async def setup(bot: commands.Bot) -> None:
    await bot.add_cog(AuthCog(bot))
