"""
서버 레지스트리 / 생애주기 명령 (T21) — server_lifecycle.md 구현.

봇은 디스코드 측만 만진다(§0): 레지스트리 상태 + 카테고리/역할 가시성 + 게이팅.
게임 서버 기동/종료·화이트리스트는 게임 호스팅 소관(봇 권위 아님, DL-133).

구현(§1 상태머신 prep→active→ended):
  - 🟢 /서버목록 · /서버정보            (조회)
  - 🟢 /서버신설                        (prep 행 생성 + T17 템플릿 자동 전개:
                                          접근역할 + 카테고리 + 채널 세트 생성, prep=비공개.
                                          수동 category 지정 시 자동전개 생략·기존 연결)
  - 🟢 /서버시작(prep→active) · /서버종료(active→ended)  (상태 전이 + 카테고리 가시성)

카테고리 가시성: 행에 category_id·access_role_id 가 있으면 시작=접근역할에 공개,
종료=숨김(아카이브, 채널·역할 보존 + 이름 `[종료]` 프리픽스). 미지정이면 상태만 전이.
게이팅 3층 = core/gating.py(도메인 명령에 부착).

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

from core import mod_log, servers
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
        embed.add_field(name="도메인", value=f"`{r['domain']}`", inline=True)
        embed.add_field(name="시즌", value=str(r["season_no"]), inline=True)
        embed.add_field(name="상태", value=_STATE_LABEL.get(r["state"], r["state"]), inline=True)
        embed.add_field(name="카테고리 ID", value=str(r["category_id"] or "—"), inline=True)
        embed.add_field(name="접근역할 ID", value=str(r["access_role_id"] or "—"), inline=True)
        embed.add_field(name="API env key", value=str(r["api_env_key"] or "—"), inline=True)
        await interaction.response.send_message(embed=embed, ephemeral=True)

    # ─── 신설 (레지스트리 행 생성 — 카테고리/역할 자동생성은 T17) ─────

    @app_commands.command(name="서버신설", description="새 서버(시즌)를 prep 상태로 등록 + 카테고리/채널/역할 자동 생성(T17).")
    @app_commands.describe(
        domain="게임 도메인 (예: rpg, poromon)",
        season_no="시즌 번호",
        display_name="표시명 (예: RPG 시즌3)",
        자동생성="템플릿으로 카테고리·채널·접근역할 자동 생성(기본 True). category 수동 지정 시 무시됨",
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
        created_role: discord.Role | None = None
        created_category: discord.CategoryChannel | None = None
        if auto:
            await interaction.response.defer(ephemeral=True)  # 생성은 수 초 소요 가능
            try:
                created_role, created_category = await templates.provision(
                    interaction.guild, domain=domain, display_name=display_name
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

        cat_obj = created_category or category
        role_obj = created_role or access_role
        try:
            sid = await servers.create_prep_server(
                self.db, domain, season_no, display_name,
                category_id=cat_obj.id if cat_obj else None,
                access_role_id=role_obj.id if role_obj else None,
            )
        except sqlite3.IntegrityError:
            # 레이스로 중복 등록 거부 — 자동 생성한 자산이 있으면 롤백(잔재 방지).
            if created_role or created_category:
                await templates.cleanup(created_role, created_category)
            await self._reply(
                interaction, f"중복 등록 거부: `{domain}` S{season_no} 는 이미 존재합니다.", deferred=auto
            )
            return

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
            ch_count = len(templates.template_for(domain))
            extra = (
                f"\n📁 카테고리 **{display_name}**(비공개) + 채널 {ch_count}개 + 접근역할 "
                f"**{role_obj.name}** 자동 생성. `/서버시작` 시 접근역할에게 공개됩니다."
            )
        else:
            extra = "\n(레지스트리만 등록 — 카테고리/역할 미연결)" if not cat_obj else ""
        await self._reply(
            interaction,
            f"✅ 서버 `#{sid}` 신설(🟡 준비): **{display_name}** — `{domain}` S{season_no}.{extra}\n"
            f"다음: `/서버시작 {sid}` 로 활성화.",
            deferred=auto,
        )

    # ─── 상태 전이 (prep→active→ended) ───────────────────────────────

    async def _set_category_visibility(
        self, guild: discord.Guild, row, *, visible: bool
    ) -> str:
        """행의 category_id·access_role_id 로 카테고리 가시성 토글.

        반환 = 사용자 안내용 짧은 상태 문구. (미지정/권한없음도 전이 자체는 진행)
        """
        category_id = row["category_id"]
        access_role_id = row["access_role_id"]
        if not category_id or not access_role_id:
            return "카테고리/역할 미연결(상태만 전이)"
        category = guild.get_channel(category_id)
        role = guild.get_role(access_role_id)
        if not isinstance(category, discord.CategoryChannel) or role is None:
            return "카테고리/역할 객체 없음(상태만 전이)"
        try:
            await category.set_permissions(
                role, view_channel=visible, reason="서버 생애주기 전이"
            )
            # 종료 시 [종료] 프리픽스로 아카이브 표식(채널·역할은 보존)
            if not visible and not category.name.startswith("[종료]"):
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
        # prep → active. domain당 active 1개 = 부분 유니크 인덱스가 강제.
        try:
            await servers.set_state(self.db, server_id, "active")
        except sqlite3.IntegrityError:
            active = await servers.get_active(self.db, row["domain"])
            await interaction.response.send_message(
                f"`{row['domain']}` 도메인에 이미 활성 시즌이 있습니다"
                f"(`#{active['id'] if active else '?'}`). 먼저 종료하세요.",
                ephemeral=True,
            )
            return
        note = await self._set_category_visibility(interaction.guild, row, visible=True) if interaction.guild else "길드 없음"
        log.info("서버 시작: #%d (%s) by %s", server_id, row["domain"], interaction.user.id)
        await mod_log.record(
            self.bot,
            action="server_start",
            operator_id=interaction.user.id,
            detail={"server_id": server_id, "domain": row["domain"], "display_name": row["display_name"]},
        )
        await interaction.response.send_message(
            f"🟢 서버 `#{server_id}` **{row['display_name']}** 활성화 완료. ({note})", ephemeral=True
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
        await servers.set_state(self.db, server_id, "ended", ended_at=int(time.time()))
        note = await self._set_category_visibility(interaction.guild, row, visible=False) if interaction.guild else "길드 없음"
        log.info(
            "서버 종료: #%d (%s) reason=%r by %s",
            server_id, row["domain"], reason, interaction.user.id,
        )
        await mod_log.record(
            self.bot,
            action="server_end",
            operator_id=interaction.user.id,
            reason=reason,
            detail={"server_id": server_id, "domain": row["domain"], "display_name": row["display_name"]},
        )
        await interaction.response.send_message(
            f"⚫ 서버 `#{server_id}` **{row['display_name']}** 종료(아카이브). 사유: {reason} ({note})",
            ephemeral=True,
        )


async def setup(bot: commands.Bot) -> None:
    await bot.add_cog(ServerLifecycleCog(bot))
