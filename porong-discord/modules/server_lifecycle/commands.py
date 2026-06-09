"""
서버 레지스트리 / 생애주기 명령 (T21) — server_lifecycle.md 구현.

봇은 디스코드 측만 만진다(§0): 레지스트리 상태 + 카테고리/역할 가시성 + 게이팅.
게임 서버 기동/종료·화이트리스트는 게임 호스팅 소관(봇 권위 아님, DL-133).

구현(§1 상태머신 prep→active→ended):
  - 🟢 /서버목록 · /서버정보            (조회)
  - 🟢 /서버신설                        (prep 행 생성 + T17 템플릿 자동 전개:
                                          온보딩 3역할 + 프리픽스 카테고리 4그룹 + 채널 세트,
                                          전부 prep=비공개. 수동 category 지정 시 자동전개 생략)
  - 🟢 /서버시작(prep→active) · /서버종료(active→ended)  (상태 전이 + 카테고리 가시성)

카테고리 가시성(templates.apply_visibility): 자동전개면 약관→접근 / 인증→인증전 /
그 외 카테고리→플레이어 에게 시작 시 공개, 종료 시 전부 숨김 + `[종료]` 프리픽스(채널·역할 보존).
수동연결(단수 category_id+access_role_id)은 단일 카테고리 접근역할 토글(폴백).
게이팅 3층 = core/gating.py(다중 카테고리 집합 매칭). 도메인 명령에 부착.

모든 명령 `@requires_permission("admin")`. 응답 ephemeral.
신설/시작/종료 전이는 core/mod_log.record() 로 운영로그(mod_log + #운영로그)에 적재한다(§2).
TODO: 이모지 서버선택 패널 항목 추가/제거(§3) — 패널 구현 후.
"""
from __future__ import annotations

import logging
import sqlite3
import time

import discord
from discord import app_commands
from discord.ext import commands

from core import config, mod_log, servers
from core.permissions import requires_permission
from modules.server_lifecycle import templates

log = logging.getLogger(__name__)

_STATE_LABEL = {"prep": "🟡 준비", "active": "🟢 활성", "ended": "⚫ 종료"}


class ServerLifecycleCog(commands.Cog):
    def __init__(self, bot: commands.Bot) -> None:
        self.bot = bot

    @property
    def db(self):
        return self.bot.db  # type: ignore[attr-defined]

    @staticmethod
    async def _reply(interaction: discord.Interaction, content: str, *, deferred: bool) -> None:
        """defer 여부에 맞춰 응답(자동전개는 defer 후 followup, 그 외 즉시 response)."""
        if deferred:
            await interaction.followup.send(content, ephemeral=True)
        else:
            await interaction.response.send_message(content, ephemeral=True)

    # ─── 전역 단일 active 모델 — 입장 자동배정 / 일괄 역할 전이 (task.md §5) ──

    @commands.Cog.listener()
    async def on_member_join(self, member: discord.Member) -> None:
        """입장 시 자동 배정: active 서버 있으면 그 서버 임시(접근)역할, 없으면 서버준비."""
        if member.bot:
            return
        active = await servers.get_any_active(self.db)
        role_id = active["access_role_id"] if active else config.ROLE_서버준비_ID
        role = member.guild.get_role(role_id or 0)
        if role is None:
            return
        try:
            await member.add_roles(role, reason="입장 자동 배정(전역 단일 active 모델)")
        except discord.Forbidden:
            log.warning("입장 자동배정 권한 부족: member=%s role=%s", member.id, role_id)

    async def _bulk_transition(
        self,
        guild: discord.Guild,
        *,
        remove_role_ids: list[int],
        add_role_id: int,
        reason: str,
    ) -> int:
        """전체 멤버 순회 — remove_role_ids 회수 + add_role_id 부여. 부여한 인원 수 반환.

        ⚠ 인원 많으면 레이트리밋으로 수십초~분 소요(호출부에서 defer 필수).
        """
        add_role = guild.get_role(add_role_id) if add_role_id else None
        remove_roles = [r for r in (guild.get_role(rid) for rid in remove_role_ids if rid) if r]
        granted = 0
        for m in guild.members:
            if m.bot:
                continue
            try:
                to_remove = [r for r in remove_roles if r in m.roles]
                if to_remove:
                    await m.remove_roles(*to_remove, reason=reason)
                if add_role is not None and add_role not in m.roles:
                    await m.add_roles(add_role, reason=reason)
                    granted += 1
            except discord.Forbidden:
                continue
        return granted

    async def _integrated_set(self, guild: discord.Guild, role_id: int, *, visible: bool) -> None:
        """통합 카테고리(공지·채팅)에 대한 역할 가시성 토글(베스트에포트)."""
        cat = guild.get_channel(config.CATEGORY_통합_ID) if config.CATEGORY_통합_ID else None
        role = guild.get_role(role_id) if role_id else None
        if not isinstance(cat, discord.CategoryChannel) or role is None:
            return
        try:
            await cat.set_permissions(role, view_channel=visible, reason="통합 카테고리 가시성")
        except discord.Forbidden:
            log.warning("통합 카테고리 권한 부족: role=%s", role_id)

    # ─── 조회 (읽기) ──────────────────────────────────────────────

    @app_commands.command(name="서버목록", description="등록된 서버(시즌) 레지스트리를 조회합니다.")
    @requires_permission("admin")
    async def server_list(self, interaction: discord.Interaction) -> None:
        rows = await servers.list_servers(self.db)
        if not rows:
            await interaction.response.send_message(
                "등록된 서버가 없습니다. `/서버신설` 로 추가하세요.", ephemeral=True
            )
            return
        lines = [
            f"`#{r['id']}` {_STATE_LABEL.get(r['state'], r['state'])} "
            f"**{r['display_name']}** — `{r['domain']}` S{r['season_no']}"
            for r in rows
        ]
        embed = discord.Embed(
            title="서버 레지스트리",
            description="\n".join(lines),
            color=discord.Color.blurple(),
        )
        await interaction.response.send_message(embed=embed, ephemeral=True)

    @app_commands.command(name="서버정보", description="단일 서버의 상세 정보를 조회합니다.")
    @app_commands.describe(server_id="서버 ID(/서버목록 의 #번호)")
    @requires_permission("admin")
    async def server_info(self, interaction: discord.Interaction, server_id: int) -> None:
        r = await servers.get_server(self.db, server_id)
        if r is None:
            await interaction.response.send_message(
                f"서버 `#{server_id}` 를 찾을 수 없습니다.", ephemeral=True
            )
            return
        embed = discord.Embed(
            title=f"서버 #{r['id']} — {r['display_name']}",
            color=discord.Color.blurple(),
        )
        cats = await servers.get_categories(self.db, server_id)
        embed.add_field(name="도메인", value=f"`{r['domain']}`", inline=True)
        embed.add_field(name="시즌", value=str(r["season_no"]), inline=True)
        embed.add_field(name="상태", value=_STATE_LABEL.get(r["state"], r["state"]), inline=True)
        if cats:  # 자동전개 — 다중 카테고리 + 3역할
            cat_str = ", ".join(f"{c['group_key']}=`{c['category_id']}`" for c in cats)
            embed.add_field(name=f"카테고리({len(cats)})", value=cat_str[:1024], inline=False)
            embed.add_field(
                name="온보딩 역할",
                value=f"접근=`{r['access_role_id'] or '—'}` 인증전=`{r['pending_role_id'] or '—'}` "
                      f"플레이어=`{r['player_role_id'] or '—'}`",
                inline=False,
            )
        else:  # 수동연결 — 단수
            embed.add_field(name="카테고리 ID", value=str(r["category_id"] or "—"), inline=True)
            embed.add_field(name="접근역할 ID", value=str(r["access_role_id"] or "—"), inline=True)
        embed.add_field(name="API env key", value=str(r["api_env_key"] or "—"), inline=True)
        await interaction.response.send_message(embed=embed, ephemeral=True)

    # ─── 신설 (레지스트리 행 생성 — 카테고리/역할 자동생성은 T17) ─────

    @app_commands.command(name="서버신설", description="새 서버(시즌)를 prep 등록 + 카테고리 4그룹·채널·온보딩 3역할 자동 생성(T17).")
    @app_commands.describe(
        domain="게임 도메인 (예: rpg, poromon)",
        season_no="시즌 번호",
        display_name="표시명 (예: RPG 시즌3)",
        자동생성="템플릿으로 카테고리 4그룹·채널·온보딩 3역할 자동 생성(기본 True). category 수동 지정 시 무시됨",
        category=" (선택) 기존 카테고리 수동 연결 — 지정 시 자동생성 안 함",
        access_role=" (선택) 기존 접근(가시성) 역할 수동 연결",
    )
    @requires_permission("admin")
    async def server_create(
        self,
        interaction: discord.Interaction,
        domain: str,
        season_no: int,
        display_name: str,
        자동생성: bool = True,
        category: discord.CategoryChannel | None = None,
        access_role: discord.Role | None = None,
    ) -> None:
        existing = await servers.get_by_domain_season(self.db, domain, season_no)
        if existing is not None:
            await interaction.response.send_message(
                f"이미 존재합니다: `{domain}` S{season_no} (`#{existing['id']}`).", ephemeral=True
            )
            return

        # 자동전개 = 자동생성 ON + 수동 연결(category/access_role) 미지정 + 길드 컨텍스트.
        auto = 자동생성 and category is None and access_role is None and interaction.guild is not None
        created_roles: dict[str, discord.Role] = {}
        created_categories: dict[str, discord.CategoryChannel] = {}
        if auto:
            await interaction.response.defer(ephemeral=True)  # 생성은 수 초 소요 가능
            try:
                created_roles, created_categories = await templates.provision(
                    interaction.guild, display_name=display_name
                )
            except discord.Forbidden:
                await interaction.followup.send(
                    "봇 권한 부족(Manage Channels/Roles)으로 자동 생성 실패. "
                    "권한 부여 후 재시도하거나 `자동생성:False` 로 레지스트리만 등록하세요.",
                    ephemeral=True,
                )
                return
            except discord.HTTPException:
                log.exception("T17 템플릿 전개 실패: %s S%d", domain, season_no)
                await interaction.followup.send("카테고리/채널 생성 중 오류가 발생했습니다.", ephemeral=True)
                return

        # 레지스트리 행: 자동전개면 3역할, 수동연결이면 access만. 단수 category_id=수동연결용.
        try:
            sid = await servers.create_prep_server(
                self.db, domain, season_no, display_name,
                category_id=category.id if category else None,
                access_role_id=(created_roles["access"].id if created_roles else (access_role.id if access_role else None)),
                pending_role_id=created_roles["pending"].id if created_roles else None,
                player_role_id=created_roles["player"].id if created_roles else None,
            )
        except sqlite3.IntegrityError:
            # 레이스로 중복 등록 거부 — 자동 생성한 자산이 있으면 롤백(잔재 방지).
            if created_roles or created_categories:
                await templates.cleanup(created_roles, created_categories)
            await self._reply(
                interaction, f"중복 등록 거부: `{domain}` S{season_no} 는 이미 존재합니다.", deferred=auto
            )
            return

        # 자동전개 카테고리들을 server_categories 에 연결.
        for group_key, cat in created_categories.items():
            await servers.add_category(self.db, sid, group_key, cat.id)

        log.info(
            "서버 신설(prep): #%d %s (%s S%d) auto=%s by %s",
            sid, display_name, domain, season_no, auto, interaction.user.id,
        )
        await mod_log.record(
            self.bot,
            action="server_create",
            operator_id=interaction.user.id,
            detail={
                "server_id": sid, "domain": domain, "season": season_no,
                "display_name": display_name, "auto_provisioned": bool(auto),
            },
        )
        if auto:
            extra = (
                f"\n📁 카테고리 {templates.group_count()}개(온보딩·정보·커뮤니티·지원·음성) + "
                f"채널 {templates.channel_count()}개 + 온보딩 3역할(접근·인증전·플레이어) 자동 생성(비공개).\n"
                "`/서버시작` 시 약관→접근 / 인증→인증전 / 그 외→플레이어 에게 공개됩니다."
            )
        else:
            extra = "\n(레지스트리만 등록 — 카테고리/역할 미연결)" if not category else ""
        await self._reply(
            interaction,
            f"✅ 서버 `#{sid}` 신설(🟡 준비): **{display_name}** — `{domain}` S{season_no}.{extra}\n"
            f"다음: `/서버시작 {sid}` 로 활성화.",
            deferred=auto,
        )

    # ─── 상태 전이 (prep→active→ended) ───────────────────────────────

    async def _set_category_visibility(
        self, guild: discord.Guild, row, *, visible: bool, archive: bool = False
    ) -> str:
        """서버 카테고리 가시성 토글. 반환 = 사용자 안내용 짧은 문구.

        자동전개(다중 카테고리 + 3역할) = templates.apply_visibility(약관→접근/인증→인증전/그외→플레이어).
        수동연결(단수 category_id + access_role_id) = 단일 카테고리 접근역할 토글(구 동작).
        """
        cats = await servers.get_categories(self.db, row["id"])
        if cats:  # 자동전개 — 다중 카테고리 그룹
            return await templates.apply_visibility(
                guild,
                category_ids={r["group_key"]: r["category_id"] for r in cats},
                role_ids={
                    "access": row["access_role_id"] or 0,
                    "pending": row["pending_role_id"] or 0,
                    "player": row["player_role_id"] or 0,
                },
                visible=visible,
                archive=archive,
            )

        # 수동연결 폴백 — 단일 카테고리 + 접근역할
        category_id = row["category_id"]
        access_role_id = row["access_role_id"]
        if not category_id or not access_role_id:
            return "카테고리/역할 미연결(상태만 전이)"
        category = guild.get_channel(category_id)
        role = guild.get_role(access_role_id)
        if not isinstance(category, discord.CategoryChannel) or role is None:
            return "카테고리/역할 객체 없음(상태만 전이)"
        try:
            await category.set_permissions(role, view_channel=visible, reason="서버 생애주기 전이")
            if archive and not visible and not category.name.startswith("[종료]"):
                await category.edit(name=f"[종료] {category.name}", reason="서버 종료 아카이브")
            return "카테고리 가시성 갱신 완료"
        except discord.Forbidden:
            return "⚠ 봇 권한 부족(Manage Channels/Roles) — 가시성 미반영"

    @app_commands.command(name="서버시작", description="prep 서버를 활성화합니다(prep→active).")
    @app_commands.describe(server_id="서버 ID(/서버목록 의 #번호)")
    @requires_permission("admin")
    async def server_start(self, interaction: discord.Interaction, server_id: int) -> None:
        row = await servers.get_server(self.db, server_id)
        if row is None:
            await interaction.response.send_message(f"서버 `#{server_id}` 없음.", ephemeral=True)
            return
        if row["state"] == "active":
            await interaction.response.send_message("이미 활성 상태입니다.", ephemeral=True)
            return
        if row["state"] == "ended":
            await interaction.response.send_message(
                "종료된 서버는 재시작할 수 없습니다(새 시즌은 새로 신설).", ephemeral=True
            )
            return
        # 전역 단일 active 강제(v6) — 다른 active 서버가 있으면 먼저 종료.
        other = await servers.get_any_active(self.db)
        if other is not None:
            await interaction.response.send_message(
                f"이미 활성 서버가 있습니다: `#{other['id']}` **{other['display_name']}**. "
                "전역 1개만 운영합니다 — 먼저 `/서버종료` 하세요.",
                ephemeral=True,
            )
            return

        # 일괄 역할 전이는 시간이 걸릴 수 있어 defer.
        await interaction.response.defer(ephemeral=True)
        try:
            await servers.set_state(self.db, server_id, "active")
        except sqlite3.IntegrityError:
            await interaction.followup.send(
                "활성화 실패 — 이미 다른 활성 서버가 있습니다(전역 1개).", ephemeral=True
            )
            return

        note = await self._set_category_visibility(interaction.guild, row, visible=True) if interaction.guild else "길드 없음"
        granted = 0
        if interaction.guild:
            # 통합 카테고리를 이 시즌 플레이어 역할에 공개(인증 완료자가 통합도 보이도록).
            await self._integrated_set(interaction.guild, row["player_role_id"] or 0, visible=True)
            # 서버준비 회수 + 시즌 임시(접근)역할 일괄 부여 → 온보딩 재시작.
            granted = await self._bulk_transition(
                interaction.guild,
                remove_role_ids=[config.ROLE_서버준비_ID],
                add_role_id=row["access_role_id"] or 0,
                reason=f"서버시작 #{server_id} — 임시역할 일괄 부여",
            )

        log.info("서버 시작: #%d (%s) granted=%d by %s", server_id, row["domain"], granted, interaction.user.id)
        await mod_log.record(
            self.bot,
            action="server_start",
            operator_id=interaction.user.id,
            detail={"server_id": server_id, "domain": row["domain"],
                    "display_name": row["display_name"], "임시부여": granted},
        )
        await interaction.followup.send(
            f"🟢 서버 `#{server_id}` **{row['display_name']}** 활성화 완료. ({note})\n"
            f"임시역할 일괄 부여: **{granted}명** (서버준비 회수).",
            ephemeral=True,
        )

    @app_commands.command(name="서버종료", description="활성 서버를 종료합니다(active→ended, 아카이브).")
    @app_commands.describe(server_id="서버 ID", reason="종료 사유(운영로그용)")
    @requires_permission("admin")
    async def server_end(
        self, interaction: discord.Interaction, server_id: int, reason: str
    ) -> None:
        row = await servers.get_server(self.db, server_id)
        if row is None:
            await interaction.response.send_message(f"서버 `#{server_id}` 없음.", ephemeral=True)
            return
        if row["state"] == "ended":
            await interaction.response.send_message("이미 종료된 서버입니다.", ephemeral=True)
            return
        if row["state"] == "prep":
            await interaction.response.send_message(
                "준비(prep) 상태는 종료 대상이 아닙니다(prep→active→ended).", ephemeral=True
            )
            return

        # 일괄 역할 전이는 시간이 걸릴 수 있어 defer.
        await interaction.response.defer(ephemeral=True)
        await servers.set_state(self.db, server_id, "ended", ended_at=int(time.time()))
        note = await self._set_category_visibility(interaction.guild, row, visible=False, archive=True) if interaction.guild else "길드 없음"
        granted = 0
        if interaction.guild:
            # 통합 카테고리에서 이 시즌 플레이어 역할 가시성 제거 + 서버준비에게 통합 공개.
            await self._integrated_set(interaction.guild, row["player_role_id"] or 0, visible=False)
            await self._integrated_set(interaction.guild, config.ROLE_서버준비_ID, visible=True)
            # 시즌 3역할 일괄 회수 + 서버준비 일괄 부여 → 시즌 사이 대기 상태.
            granted = await self._bulk_transition(
                interaction.guild,
                remove_role_ids=[
                    row["access_role_id"] or 0,
                    row["pending_role_id"] or 0,
                    row["player_role_id"] or 0,
                ],
                add_role_id=config.ROLE_서버준비_ID,
                reason=f"서버종료 #{server_id} — 시즌 역할 회수·서버준비 부여",
            )

        log.info(
            "서버 종료: #%d (%s) reason=%r granted=%d by %s",
            server_id, row["domain"], reason, granted, interaction.user.id,
        )
        await mod_log.record(
            self.bot,
            action="server_end",
            operator_id=interaction.user.id,
            reason=reason,
            detail={"server_id": server_id, "domain": row["domain"],
                    "display_name": row["display_name"], "서버준비부여": granted},
        )
        await interaction.followup.send(
            f"⚫ 서버 `#{server_id}` **{row['display_name']}** 종료(아카이브). 사유: {reason} ({note})\n"
            f"서버준비 일괄 부여: **{granted}명** (시즌 역할 회수).",
            ephemeral=True,
        )


async def setup(bot: commands.Bot) -> None:
    await bot.add_cog(ServerLifecycleCog(bot))
