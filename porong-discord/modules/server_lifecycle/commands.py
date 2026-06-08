"""
서버 레지스트리 / 생애주기 명령 (T21) — server_lifecycle.md 구현.

봇은 디스코드 측만 만진다(§0): 레지스트리 상태 + 카테고리/역할 가시성 + 게이팅.
게임 서버 기동/종료·화이트리스트는 게임 호스팅 소관(봇 권위 아님, DL-133).

이번 슬라이스(읽기 우선, §12.3):
  - 🟢 /서버목록 · /서버정보  (조회)
  - 🟡 /서버신설            (레지스트리 prep 행 생성까지. 카테고리/역할 자동생성=T17 후속)
  - ⬜ /서버시작 · /서버종료  (상태 전이 + 카테고리 가시성 — 다음 단계, 명시 요청 시)

모든 명령 `@requires_permission("admin")`. 응답 ephemeral.
TODO: 전이/신설 시 mod_log 적재 + #운영로그 게시(§2) — mod_log 인프라 도입 후.
"""
from __future__ import annotations

import logging
import sqlite3

import discord
from discord import app_commands
from discord.ext import commands

from core import servers
from core.permissions import requires_permission

log = logging.getLogger(__name__)

_STATE_LABEL = {"prep": "🟡 준비", "active": "🟢 활성", "ended": "⚫ 종료"}


class ServerLifecycleCog(commands.Cog):
    def __init__(self, bot: commands.Bot) -> None:
        self.bot = bot

    @property
    def db(self):
        return self.bot.db  # type: ignore[attr-defined]

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

    @app_commands.command(name="서버신설", description="새 서버(시즌)를 prep 상태로 레지스트리에 등록합니다.")
    @app_commands.describe(
        domain="게임 도메인 (예: rpg, poromon)",
        season_no="시즌 번호",
        display_name="표시명 (예: RPG 시즌3)",
    )
    @requires_permission("admin")
    async def server_create(
        self,
        interaction: discord.Interaction,
        domain: str,
        season_no: int,
        display_name: str,
    ) -> None:
        existing = await servers.get_by_domain_season(self.db, domain, season_no)
        if existing is not None:
            await interaction.response.send_message(
                f"이미 존재합니다: `{domain}` S{season_no} (`#{existing['id']}`).", ephemeral=True
            )
            return
        try:
            sid = await servers.create_prep_server(self.db, domain, season_no, display_name)
        except sqlite3.IntegrityError:
            await interaction.response.send_message(
                f"중복 등록 거부: `{domain}` S{season_no} 는 이미 존재합니다.", ephemeral=True
            )
            return

        log.info(
            "서버 신설(prep): #%d %s (%s S%d) by %s",
            sid, display_name, domain, season_no, interaction.user.id,
        )
        await interaction.response.send_message(
            f"✅ 서버 `#{sid}` 신설(🟡 준비): **{display_name}** — `{domain}` S{season_no}.\n"
            "※ 카테고리·접근역할 자동 생성과 `/서버시작` 활성화는 후속(T17/상태전이).",
            ephemeral=True,
        )


async def setup(bot: commands.Bot) -> None:
    await bot.add_cog(ServerLifecycleCog(bot))
