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
  - 닉 입력 폐기 → verify 응답 uuid+name 으로 봇이 신원 확보. 매핑은 봇 DB 에
    저장하지 않는다(권위=게임서버, data_model.md §3) — name 은 표시/로깅 용도.

채널은 읽기전용(send + use_application_commands 차단) 권장. 버튼/모달은
읽기전용에서도 작동한다. 봇 측 역할 게이트는 우회 방어로 병행한다.
"""
from __future__ import annotations

import logging

import discord
from discord import app_commands
from discord.ext import commands

from core import config, mod_log, terms
from core.permissions import requires_permission
from integrations.poromon_api import PoromonApiClient, PoromonAuthError

log = logging.getLogger(__name__)

# 사용자 안내 문구
_MSG_TERMS_OK = "✅ 약관에 동의했습니다. 이제 **인증 채널**에서 인증코드를 입력해주세요."
_MSG_SUCCESS = "✅ 인증이 완료되었습니다! 이제 서버에서 정상적으로 활동할 수 있습니다."
_MSG_NOT_FOUND = "코드가 올바르지 않거나 만료되었습니다. 인게임에서 코드를 다시 발급해주세요."
_MSG_RATE_LIMITED = "인증 시도가 너무 많습니다. 잠시 후 다시 시도해주세요."
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


# ─── 약관 입력/수정 모달 (운영자) ────────────────────────────────

class TermsEditModal(discord.ui.Modal, title="약관 설정"):
    """운영자가 서버 약관 내용을 입력/수정. 저장은 core.terms(DB)."""

    def __init__(self, domain: str, existing: str | None, cog: "OnboardingCog") -> None:
        super().__init__()
        self.domain = domain
        self.cog = cog
        self.content: discord.ui.TextInput = discord.ui.TextInput(
            label=f"{domain} 약관 내용"[:45],
            style=discord.TextStyle.paragraph,
            default=existing or "",
            max_length=4000,
            required=True,
        )
        self.add_item(self.content)

    async def on_submit(self, interaction: discord.Interaction) -> None:
        await self.cog.save_terms(self.domain, interaction, self.content.value)


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
            if result.get("reason") == "rate_limited":
                return _MSG_RATE_LIMITED
            return _MSG_NOT_FOUND

        await self._promote(domain, guild, user)
        # 매핑(discord_id↔mc)은 봇 DB 에 저장하지 않음 — 권위=게임서버(data_model.md §3).
        # name 은 로깅/표시 용도로만 사용(영속 상태 = 부여된 디스코드 역할).
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

    # ─── 운영자: 약관 입력/조회 ───────────────────────────────────

    @app_commands.command(name="약관설정", description="서버 약관 내용을 입력/수정합니다(모달).")
    @app_commands.describe(server="서버 도메인 (예: rpg, poromon)")
    @requires_permission("admin")
    async def set_terms_cmd(self, interaction: discord.Interaction, server: str) -> None:
        existing = await terms.get_terms(self.bot.db, server)  # type: ignore[attr-defined]
        await interaction.response.send_modal(TermsEditModal(server, existing, self))

    async def save_terms(
        self, domain: str, interaction: discord.Interaction, content: str
    ) -> None:
        """모달 제출 콜백 — 약관 저장 + 운영로그 적재."""
        content = (content or "").strip()
        if not content:
            await interaction.response.send_message("약관 내용이 비어 있습니다.", ephemeral=True)
            return
        await terms.set_terms(self.bot.db, domain, content, interaction.user.id)  # type: ignore[attr-defined]
        await mod_log.record(
            self.bot, action="terms_update", operator_id=interaction.user.id,
            detail={"domain": domain, "length": len(content)},
        )
        await interaction.response.send_message(
            f"✅ `{domain}` 약관 저장({len(content)}자). `/온보딩패널 {domain}` 으로 게시·갱신하세요.",
            ephemeral=True,
        )

    @app_commands.command(name="약관보기", description="저장된 서버 약관을 확인합니다(본인만).")
    @app_commands.describe(server="서버 도메인 (예: rpg, poromon)")
    @requires_permission("admin")
    async def view_terms_cmd(self, interaction: discord.Interaction, server: str) -> None:
        content = await terms.get_terms(self.bot.db, server)  # type: ignore[attr-defined]
        if not content:
            await interaction.response.send_message(
                f"`{server}` 약관이 아직 설정되지 않았습니다. `/약관설정 {server}` 로 입력하세요.",
                ephemeral=True,
            )
            return
        embed = discord.Embed(
            title=f"{server} 약관 (저장본)", description=content[:4096], color=discord.Color.teal()
        )
        await interaction.response.send_message(embed=embed, ephemeral=True)

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
            stored = await terms.get_terms(self.bot.db, server)  # type: ignore[attr-defined]
            guide = (
                "\n\n— 아래 **약관 동의** 버튼을 누르면 인증 단계로 넘어갑니다. "
                "동의 후 인증 채널에서 인게임 `/인증` 코드를 입력해주세요."
            )
            if stored:
                description = stored[: 4096 - len(guide)] + guide
            else:
                description = (
                    f"⚠ 약관이 아직 설정되지 않았습니다(`/약관설정 {server}`).{guide}"
                )
            embed = discord.Embed(
                title=f"{name} 서버 약관 동의",
                description=description,
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
