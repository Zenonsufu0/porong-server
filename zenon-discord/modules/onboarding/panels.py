"""
공통 멀티서버 온보딩 — 약관동의 게이트 + 인증 버튼/모달 패널 (DB 기반).

전역 단일 active 서버 모델(task.md §5, 2026-06-09): 진행 중 서버는 항상 1개.
온보딩은 그 active 서버(`servers.get_any_active`)의 **3역할 + 온보딩 카테고리 채널**을
DB 레지스트리에서 읽는다(구 `ONBOARDING_SERVERS` env 매핑 SUPERSEDE).

흐름 (3단계 역할 상태머신 — 채널 가시성 = 역할 권한 오버라이트):
  입장 → [자동] active 서버 `임시`(접근)역할 부여(server_lifecycle.on_member_join)
  1. 약관 채널([약관 동의] 버튼) → 접근 → `인증전` → 인증 채널만 보임(약관 숨김)
  2. 인증 채널([🔑 인증코드 입력] 버튼 → 모달) → 게임서버 verify → `플레이어`(정식)
     → 온보딩(임시) 카테고리 사라지고 나머지 카테고리 + 통합 보임

설계 결정(task.md §5):
  - 코드 방향 = 인게임 `/인증` 발급 → 봇 검증. UI = 버튼+모달(채팅 깔끔).
  - 약관동의 게이트 = 역할 기반(인증전 역할 확인).
  - 매핑(discord_id↔mc)은 봇 DB 미저장(권위=게임서버, data_model §3) — name 은 표시/로깅용.

verify 라우팅: 도메인 → verifier. 포로몬 구현됨. 미등록 도메인(rpg 등)은 graceful 안내.
"""
from __future__ import annotations

import logging

import discord
from discord import app_commands
from discord.ext import commands

from core import mod_log, servers, terms
from core.permissions import requires_permission
from integrations.common import VerifyError
from integrations.poromon_api import PoromonApiClient
from integrations.rpg_api import PorongApiClient

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
_MSG_NO_ACTIVE = "현재 진행 중인 서버가 없습니다. 운영진에게 문의해주세요."
_MSG_NO_VERIFIER = "이 서버는 인증 연동이 아직 설정되지 않았습니다. 운영진에게 문의해주세요."


# ─── 약관 동의 버튼 뷰 (DB active 서버 기반) ──────────────────────

class TermsAgreeView(discord.ui.View):
    """약관 채널 패널 — 동의 시 접근(임시) → 인증전 역할 전이. active 서버를 DB에서 해석."""

    def __init__(self, cog: "OnboardingCog") -> None:
        super().__init__(timeout=None)  # 영구 뷰
        self.cog = cog
        btn = discord.ui.Button(
            label="약관 동의", style=discord.ButtonStyle.green, custom_id="onb_terms"
        )
        btn.callback = self._on_click
        self.add_item(btn)

    async def _on_click(self, interaction: discord.Interaction) -> None:
        row = await servers.get_any_active(self.cog.db)
        guild = interaction.guild
        member = guild.get_member(interaction.user.id) if guild else None
        if row is None or guild is None or member is None:
            await interaction.response.send_message(
                _MSG_NO_ACTIVE if row is None else _MSG_ERROR, ephemeral=True
            )
            return

        access = guild.get_role(row["access_role_id"] or 0)
        pending = guild.get_role(row["pending_role_id"] or 0)
        if pending is None:
            await interaction.response.send_message(_MSG_ERROR, ephemeral=True)
            return
        try:
            if pending not in member.roles:
                await member.add_roles(pending, reason=f"{row['domain']} 약관 동의 — 인증전")
            if access and access in member.roles:
                await member.remove_roles(access, reason=f"{row['domain']} 약관 동의 — 접근 해제")
        except discord.Forbidden:
            log.warning("약관 동의 역할 전이 권한 없음 (discord=%s)", member.id)
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


# ─── 인증코드 입력: 버튼 → 모달 (DB active 서버 기반) ─────────────

class AuthCodeModal(discord.ui.Modal, title="인증코드 입력"):
    code = discord.ui.TextInput(
        label="인증 코드",
        placeholder="인게임 /인증 으로 발급받은 코드",
        min_length=1,
        max_length=32,
    )

    def __init__(self, cog: "OnboardingCog") -> None:
        super().__init__()
        self.cog = cog

    async def on_submit(self, interaction: discord.Interaction) -> None:
        await interaction.response.defer(ephemeral=True)
        msg = await self.cog.handle_auth_submit(interaction, self.code.value)
        await interaction.followup.send(msg, ephemeral=True)


class AuthPanelView(discord.ui.View):
    """인증 채널 패널 — 버튼 클릭 시 인증코드 입력 모달."""

    def __init__(self, cog: "OnboardingCog") -> None:
        super().__init__(timeout=None)  # 영구 뷰
        self.cog = cog
        btn = discord.ui.Button(
            label="🔑 인증코드 입력", style=discord.ButtonStyle.blurple, custom_id="onb_auth"
        )
        btn.callback = self._on_click
        self.add_item(btn)

    async def _on_click(self, interaction: discord.Interaction) -> None:
        await interaction.response.send_modal(AuthCodeModal(self.cog))


# ─── Onboarding Cog ──────────────────────────────────────────────

class OnboardingCog(commands.Cog):
    def __init__(self, bot: commands.Bot) -> None:
        self.bot = bot
        self._poromon_api = PoromonApiClient()
        self._rpg_api = PorongApiClient()
        # 도메인 → verify(code, discord_id) -> {"ok","uuid","name"} / 예외 VerifyError
        self._verifiers = {
            "poromon": self._poromon_api.verify_code,
            "rpg": self._rpg_api.verify_code,
        }

    @property
    def db(self):
        return self.bot.db  # type: ignore[attr-defined]

    async def cog_unload(self) -> None:
        await self._poromon_api.close()
        await self._rpg_api.close()

    @commands.Cog.listener()
    async def on_ready(self) -> None:
        # 영구 뷰 재등록(재시작 후에도 기존 패널 버튼 동작). 전역 단일 → 뷰 1쌍.
        self.bot.add_view(TermsAgreeView(self))
        self.bot.add_view(AuthPanelView(self))

    # ─── 온보딩 카테고리 채널 탐색 (이름 매칭) ────────────────────

    async def _onboarding_channel(
        self, guild: discord.Guild, row, name: str
    ) -> discord.TextChannel | None:
        """active 서버의 온보딩 카테고리에서 이름이 일치하는 텍스트 채널."""
        cats = await servers.get_categories(self.db, row["id"])
        cat_id = next((c["category_id"] for c in cats if c["group_key"] == "onboarding"), None)
        cat = guild.get_channel(cat_id) if cat_id else None
        if isinstance(cat, discord.CategoryChannel):
            for ch in cat.channels:
                if isinstance(ch, discord.TextChannel) and ch.name == name:
                    return ch
        return None

    # ─── 인증 제출 처리 (약관 게이트 → verify → 승급) ──────────────

    async def handle_auth_submit(self, interaction: discord.Interaction, code: str) -> str:
        row = await servers.get_any_active(self.db)
        if row is None:
            return _MSG_NO_ACTIVE

        guild = interaction.guild or self.bot.get_guild(int(interaction.guild_id or 0))
        member = guild.get_member(interaction.user.id) if guild else None

        # 약관 게이트(보조 방어) — 인증전 역할 보유 확인. 이미 플레이어면 종료.
        if member is not None:
            member_role_ids = {r.id for r in member.roles}
            player_id = row["player_role_id"] or 0
            pending_id = row["pending_role_id"] or 0
            if player_id and player_id in member_role_ids:
                return _MSG_ALREADY
            if pending_id and pending_id not in member_role_ids:
                return _MSG_NEED_TERMS

        return await self._verify_and_promote(row, interaction.user, guild, code)

    async def _verify_and_promote(
        self, row, user: discord.abc.User, guild: discord.Guild | None, code: str
    ) -> str:
        code = (code or "").strip()
        if not code:
            return _MSG_EMPTY

        domain = row["domain"]
        verifier = self._verifiers.get(domain)
        if verifier is None:
            log.error("온보딩: 미등록 도메인 verifier (domain=%s)", domain)
            return _MSG_NO_VERIFIER

        try:
            result = await verifier(code, user.id)
        except VerifyError as e:
            log.error("온보딩 verify 오류 (domain=%s discord=%s): %s", domain, user.id, e)
            return _MSG_ERROR

        if not result.get("ok"):
            if result.get("reason") == "rate_limited":
                return _MSG_RATE_LIMITED
            return _MSG_NOT_FOUND

        await self._promote(row, guild, user)
        # 매핑은 봇 DB 미저장(권위=게임서버, data_model §3). name 은 로깅/표시용.
        log.info(
            "온보딩 인증 완료: domain=%s discord=%s uuid=%s name=%s",
            domain, user.id, result.get("uuid"), result.get("name"),
        )
        return _MSG_SUCCESS

    async def _promote(self, row, guild: discord.Guild | None, user: discord.abc.User) -> None:
        """인증전 → 플레이어(정식) 역할 전이."""
        if guild is None:
            log.warning("승급 불가 — 길드 컨텍스트 없음 (discord=%s)", user.id)
            return
        member = guild.get_member(user.id)
        if member is None:
            try:
                member = await guild.fetch_member(user.id)
            except discord.HTTPException:
                log.warning("승급 불가 — 길드 멤버 아님 (discord=%s)", user.id)
                return

        player = guild.get_role(row["player_role_id"] or 0)
        pending = guild.get_role(row["pending_role_id"] or 0)
        try:
            if player and player not in member.roles:
                await member.add_roles(player, reason=f"{row['domain']} 인증 완료")
            if pending and pending in member.roles:
                await member.remove_roles(pending, reason=f"{row['domain']} 인증 완료 — 인증전 해제")
        except discord.Forbidden:
            log.warning("승급 역할 부여 권한 없음 (discord=%s)", user.id)

    # ─── 운영자: 약관 입력/조회 ───────────────────────────────────

    @app_commands.command(name="약관설정", description="서버 약관 내용을 입력/수정합니다(모달).")
    @app_commands.describe(server="서버 도메인 (예: rpg, poromon)")
    @requires_permission("admin")
    async def set_terms_cmd(self, interaction: discord.Interaction, server: str) -> None:
        existing = await terms.get_terms(self.db, server)
        await interaction.response.send_modal(TermsEditModal(server, existing, self))

    async def save_terms(
        self, domain: str, interaction: discord.Interaction, content: str
    ) -> None:
        """모달 제출 콜백 — 약관 저장 + 운영로그 적재."""
        content = (content or "").strip()
        if not content:
            await interaction.response.send_message("약관 내용이 비어 있습니다.", ephemeral=True)
            return
        await terms.set_terms(self.db, domain, content, interaction.user.id)
        await mod_log.record(
            self.bot, action="terms_update", operator_id=interaction.user.id,
            detail={"domain": domain, "length": len(content)},
        )
        await interaction.response.send_message(
            f"✅ `{domain}` 약관 저장({len(content)}자). `/온보딩패널` 로 게시·갱신하세요.",
            ephemeral=True,
        )

    @app_commands.command(name="약관보기", description="저장된 서버 약관을 확인합니다(본인만).")
    @app_commands.describe(server="서버 도메인 (예: rpg, poromon)")
    @requires_permission("admin")
    async def view_terms_cmd(self, interaction: discord.Interaction, server: str) -> None:
        content = await terms.get_terms(self.db, server)
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

    # ─── 운영자: 패널 게시 (active 서버의 온보딩 채널) ────────────

    async def publish_panels(self, guild: discord.Guild, row) -> list[str]:
        """active 서버 row 의 온보딩 약관/인증 채널에 패널 게시. 게시한 곳 목록 반환.

        /온보딩패널(수동)·서버시작(자동) 공용. 채널 없으면 그 항목 skip.
        """
        name = row["display_name"]
        domain = row["domain"]
        posted: list[str] = []

        terms_ch = await self._onboarding_channel(guild, row, "약관")
        if terms_ch is not None:
            stored = await terms.get_terms(self.db, domain)
            guide = (
                "\n\n— 아래 **약관 동의** 버튼을 누르면 인증 단계로 넘어갑니다. "
                "동의 후 인증 채널에서 인게임 `/인증` 코드를 입력해주세요."
            )
            description = (
                (stored[: 4096 - len(guide)] + guide) if stored
                else f"⚠ 약관이 아직 설정되지 않았습니다(`/약관설정 {domain}`).{guide}"
            )
            embed = discord.Embed(
                title=f"{name} 서버 약관 동의", description=description, color=discord.Color.green()
            )
            await terms_ch.send(embed=embed, view=TermsAgreeView(self))
            posted.append(f"약관(<#{terms_ch.id}>)")

        auth_ch = await self._onboarding_channel(guild, row, "인증")
        if auth_ch is not None:
            embed = discord.Embed(
                title=f"{name} 인증",
                description=(
                    "인게임에서 `/인증` 을 입력해 코드를 발급받은 뒤,\n"
                    "아래 **🔑 인증코드 입력** 버튼을 눌러 코드를 입력해주세요."
                ),
                color=discord.Color.blurple(),
            )
            await auth_ch.send(embed=embed, view=AuthPanelView(self))
            posted.append(f"인증(<#{auth_ch.id}>)")
        return posted

    @app_commands.command(
        name="온보딩패널", description="진행 중 서버의 온보딩(약관/인증) 패널을 게시합니다."
    )
    @requires_permission("admin")
    async def post_panels(self, interaction: discord.Interaction) -> None:
        guild = interaction.guild
        if guild is None:
            await interaction.response.send_message("길드에서만 사용할 수 있습니다.", ephemeral=True)
            return
        row = await servers.get_any_active(self.db)
        if row is None:
            await interaction.response.send_message(
                "진행 중(active)인 서버가 없습니다. `/서버시작` 후 사용하세요.", ephemeral=True
            )
            return
        posted = await self.publish_panels(guild, row)
        if not posted:
            await interaction.response.send_message(
                "약관/인증 채널을 찾지 못했습니다(온보딩 카테고리의 `약관`·`인증` 채널 확인).",
                ephemeral=True,
            )
            return
        await interaction.response.send_message("패널 게시 완료: " + ", ".join(posted), ephemeral=True)


async def setup(bot: commands.Bot) -> None:
    await bot.add_cog(OnboardingCog(bot))
