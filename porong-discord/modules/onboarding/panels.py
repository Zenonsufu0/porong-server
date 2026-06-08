"""
공통 멀티서버 온보딩 — 약관동의 게이트 + 인증 버튼/모달 패널.

전 서버 동일 흐름(도메인 비종속). 서버별 설정은 core.config.ONBOARDING_SERVERS.

흐름 (서버별 3단계 역할 상태머신):
  1. 서버 선택 → `<서버>_접근`(임시)  → 약관 채널만 보임(읽기전용)
  2. [약관 동의] 버튼     → 임시 제거 + `<서버>_인증전` 부여 → 인증 채널만 보임
  3. [인증코드 입력] 버튼 → 모달(코드 칸) → 게임서버 verify → `<서버>_플레이어`(정식)

설계 결정(task.md §5, 2026-06-08 확정):
  - 코드 방향 = 인게임 `/인증` 발급 → 봇 검증.
  - UI = 버튼+모달(슬래시 아님) → 채팅 깔끔. 모달 입력은 본인만 보임.
  - 약관동의 게이트 = 역할 기반 1차(인증전 역할 확인). DB(T12) 불필요.
  - 닉 입력 폐기 → verify 응답 uuid+name 으로 봇이 신원 확보(영속 저장은 T12).

채널은 읽기전용(send + use_application_commands 차단) 권장. 버튼/모달은
읽기전용에서도 작동한다. 봇 측 역할 게이트는 우회 방어로 병행한다.
"""
from __future__ import annotations

import logging

import discord
from discord import app_commands
from discord.ext import commands

from core import config
from core.permissions import requires_permission
from integrations.poromon_api import PoromonApiClient, PoromonAuthError

log = logging.getLogger(__name__)

# 사용자 안내 문구
_MSG_TERMS_OK = "✅ 약관에 동의했습니다. 이제 **인증 채널**에서 인증코드를 입력해주세요."
_MSG_SUCCESS = "✅ 인증이 완료되었습니다! 이제 서버에서 정상적으로 활동할 수 있습니다."
_MSG_NOT_FOUND = "코드가 올바르지 않거나 만료되었습니다. 인게임에서 코드를 다시 발급해주세요."
_MSG_ERROR = "인증 처리 중 오류가 발생했습니다. 잠시 후 다시 시도하거나 운영진에게 문의해주세요."
_MSG_EMPTY = "인증 코드를 입력해주세요."
_MSG_NEED_TERMS = "먼저 약관에 동의해주세요. (약관 채널의 동의 버튼)"
_MSG_ALREADY = "이미 인증이 완료된 계정입니다."
_MSG_UNKNOWN = "알 수 없는 서버입니다. 운영진에게 문의해주세요."


# ─── 약관 동의 버튼 뷰 ───────────────────────────────────────────

class TermsAgreeView(discord.ui.View):
    """약관 채널 패널 — 동의 시 접근(임시) → 인증전 역할 전이."""

    def __init__(self, domain: str) -> None:
        super().__init__(timeout=None)  # 영구 뷰
        self.domain = domain
        btn = discord.ui.Button(
            label="약관 동의",
            style=discord.ButtonStyle.green,
            custom_id=f"onb_terms_{domain}",
        )
        btn.callback = self._on_click
        self.add_item(btn)

    async def _on_click(self, interaction: discord.Interaction) -> None:
        cfg = config.ONBOARDING_SERVERS.get(self.domain)
        if cfg is None:
            await interaction.response.send_message(_MSG_UNKNOWN, ephemeral=True)
            return

        guild = interaction.guild
        member = guild.get_member(interaction.user.id) if guild else None
        if guild is None or member is None:
            await interaction.response.send_message(_MSG_ERROR, ephemeral=True)
            return

        access = guild.get_role(cfg.get("access_role", 0)) if cfg.get("access_role") else None
        pending = guild.get_role(cfg.get("pending_role", 0)) if cfg.get("pending_role") else None
        try:
            if pending and pending not in member.roles:
                await member.add_roles(pending, reason=f"{self.domain} 약관 동의 — 인증전")
            if access and access in member.roles:
                await member.remove_roles(access, reason=f"{self.domain} 약관 동의 — 접근(임시) 해제")
        except discord.Forbidden:
            log.warning("약관 동의 역할 전이 권한 없음 (domain=%s discord=%s)", self.domain, member.id)
            await interaction.response.send_message(_MSG_ERROR, ephemeral=True)
            return

        await interaction.response.send_message(_MSG_TERMS_OK, ephemeral=True)


# ─── 인증코드 입력: 버튼 → 모달 ──────────────────────────────────

class AuthCodeModal(discord.ui.Modal, title="인증코드 입력"):
    code = discord.ui.TextInput(
        label="인증 코드",
        placeholder="인게임 /인증 으로 발급받은 코드",
        min_length=1,
        max_length=32,
    )

    def __init__(self, domain: str, cog: "OnboardingCog") -> None:
        super().__init__()
        self.domain = domain
        self.cog = cog

    async def on_submit(self, interaction: discord.Interaction) -> None:
        await interaction.response.defer(ephemeral=True)
        msg = await self.cog.handle_auth_submit(self.domain, interaction, self.code.value)
        await interaction.followup.send(msg, ephemeral=True)


class AuthPanelView(discord.ui.View):
    """인증 채널 패널 — 버튼 클릭 시 인증코드 입력 모달."""

    def __init__(self, domain: str, cog: "OnboardingCog") -> None:
        super().__init__(timeout=None)  # 영구 뷰
        self.domain = domain
        self.cog = cog
        btn = discord.ui.Button(
            label="🔑 인증코드 입력",
            style=discord.ButtonStyle.blurple,
            custom_id=f"onb_auth_{domain}",
        )
        btn.callback = self._on_click
        self.add_item(btn)

    async def _on_click(self, interaction: discord.Interaction) -> None:
        await interaction.response.send_modal(AuthCodeModal(self.domain, self.cog))


# ─── Onboarding Cog ──────────────────────────────────────────────

class OnboardingCog(commands.Cog):
    def __init__(self, bot: commands.Bot) -> None:
        self.bot = bot
        self._poromon_api = PoromonApiClient()
        # 도메인 → verify(code, discord_id) -> {"ok", "uuid", "name"} / 예외 PoromonAuthError
        self._verifiers = {"poromon": self._poromon_api.verify_code}

    async def cog_unload(self) -> None:
        await self._poromon_api.close()

    @commands.Cog.listener()
    async def on_ready(self) -> None:
        # 영구 뷰 재등록 (재시작 후에도 기존 패널 버튼 동작)
        for domain in config.ONBOARDING_SERVERS:
            self.bot.add_view(TermsAgreeView(domain))
            self.bot.add_view(AuthPanelView(domain, self))

    # ─── 인증 제출 처리 (약관 게이트 → verify → 승급) ──────────────

    async def handle_auth_submit(
        self, domain: str, interaction: discord.Interaction, code: str
    ) -> str:
        cfg = config.ONBOARDING_SERVERS.get(domain)
        if cfg is None:
            return _MSG_UNKNOWN

        guild = interaction.guild or self.bot.get_guild(config.GUILD_ID)
        member = guild.get_member(interaction.user.id) if guild else None

        # 약관 게이트(보조 방어) — 인증전 역할 보유 확인. 이미 플레이어면 종료.
        if member is not None:
            player_id = int(cfg.get("player_role", 0) or 0)
            pending_id = int(cfg.get("pending_role", 0) or 0)
            member_role_ids = {r.id for r in member.roles}
            if player_id and player_id in member_role_ids:
                return _MSG_ALREADY
            if pending_id and pending_id not in member_role_ids:
                return _MSG_NEED_TERMS

        return await self._verify_and_promote(domain, interaction.user, guild, code)

    async def _verify_and_promote(
        self,
        domain: str,
        user: discord.abc.User,
        guild: discord.Guild | None,
        code: str,
    ) -> str:
        code = (code or "").strip()
        if not code:
            return _MSG_EMPTY

        verifier = self._verifiers.get(domain)
        if verifier is None:
            log.error("온보딩: 미등록 도메인 verifier (domain=%s)", domain)
            return _MSG_ERROR

        try:
            result = await verifier(code, user.id)
        except PoromonAuthError as e:
            # 401/네트워크/미설정 — 운영자 확인 필요. 사용자에겐 일반 오류 안내.
            log.error("온보딩 verify 오류 (domain=%s discord=%s): %s", domain, user.id, e)
            return _MSG_ERROR

        if not result.get("ok"):
            return _MSG_NOT_FOUND

        await self._promote(domain, guild, user)
        # TODO(T12): discord_id ↔ {uuid, name} 영속 저장 (현재는 로깅만)
        log.info(
            "온보딩 인증 완료: domain=%s discord=%s uuid=%s name=%s",
            domain, user.id, result.get("uuid"), result.get("name"),
        )
        return _MSG_SUCCESS

    async def _promote(
        self, domain: str, guild: discord.Guild | None, user: discord.abc.User
    ) -> None:
        """인증전 → 플레이어(정식) 역할 전이."""
        cfg = config.ONBOARDING_SERVERS.get(domain, {})
        if guild is None:
            log.warning("승급 불가 — 길드 컨텍스트 없음 (domain=%s discord=%s)", domain, user.id)
            return

        member = guild.get_member(user.id)
        if member is None:
            try:
                member = await guild.fetch_member(user.id)
            except discord.HTTPException:
                log.warning("승급 불가 — 길드 멤버 아님 (domain=%s discord=%s)", domain, user.id)
                return

        player = guild.get_role(int(cfg.get("player_role", 0) or 0))
        pending = guild.get_role(int(cfg.get("pending_role", 0) or 0))
        try:
            if player and player not in member.roles:
                await member.add_roles(player, reason=f"{domain} 인증 완료")
            if pending and pending in member.roles:
                await member.remove_roles(pending, reason=f"{domain} 인증 완료 — 인증전 해제")
        except discord.Forbidden:
            log.warning("승급 역할 부여 권한 없음 (domain=%s discord=%s)", domain, user.id)

    # ─── 운영자: 패널 게시 ────────────────────────────────────────

    @app_commands.command(
        name="온보딩패널", description="서버 온보딩(약관/인증) 패널을 채널에 게시합니다."
    )
    @app_commands.describe(server="대상 서버 도메인 (예: poromon)")
    @requires_permission("admin")
    async def post_panels(self, interaction: discord.Interaction, server: str) -> None:
        cfg = config.ONBOARDING_SERVERS.get(server)
        if cfg is None:
            known = ", ".join(config.ONBOARDING_SERVERS) or "(없음)"
            await interaction.response.send_message(
                f"알 수 없는 서버: `{server}`. 등록된 서버: {known}", ephemeral=True
            )
            return

        guild = interaction.guild
        if guild is None:
            await interaction.response.send_message("길드에서만 사용할 수 있습니다.", ephemeral=True)
            return

        name = cfg.get("display_name", server)
        posted: list[str] = []

        terms_ch = guild.get_channel(int(cfg.get("terms_channel", 0) or 0))
        if isinstance(terms_ch, discord.TextChannel):
            embed = discord.Embed(
                title=f"{name} 서버 약관 동의",
                description=(
                    "아래 **약관 동의** 버튼을 누르면 인증 단계로 넘어갑니다.\n"
                    "동의 후 인증 채널에서 인게임 `/인증` 코드를 입력해주세요."
                ),
                color=discord.Color.green(),
            )
            await terms_ch.send(embed=embed, view=TermsAgreeView(server))
            posted.append(f"약관(<#{terms_ch.id}>)")

        auth_ch = guild.get_channel(int(cfg.get("auth_channel", 0) or 0))
        if isinstance(auth_ch, discord.TextChannel):
            embed = discord.Embed(
                title=f"{name} 인증",
                description=(
                    "인게임에서 `/인증` 을 입력해 코드를 발급받은 뒤,\n"
                    "아래 **🔑 인증코드 입력** 버튼을 눌러 코드를 입력해주세요."
                ),
                color=discord.Color.blurple(),
            )
            await auth_ch.send(embed=embed, view=AuthPanelView(server, self))
            posted.append(f"인증(<#{auth_ch.id}>)")

        if not posted:
            await interaction.response.send_message(
                f"`{server}` 의 약관/인증 채널이 설정되지 않았습니다(.env 확인).", ephemeral=True
            )
            return
        await interaction.response.send_message(
            "패널 게시 완료: " + ", ".join(posted), ephemeral=True
        )


async def setup(bot: commands.Bot) -> None:
    await bot.add_cog(OnboardingCog(bot))
