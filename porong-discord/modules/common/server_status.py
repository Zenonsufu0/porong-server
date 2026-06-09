"""
접속정보 — 진행 중 서버의 마인크래프트 상태(주소·on/off·인원·핑)를 게시·주기 갱신 (T18).

도메인 비종속: 서버는 SLP(integrations/mcping)로 핑한다 — 게임서버 커스텀 API 불필요.
전역 단일 active 모델 → active 서버 1개의 `접속정보` 채널(정보 카테고리)에 상태 임베드 1개를
유지(없으면 게시, 있으면 edit). 메시지 ID 는 servers.status_message_id 에 영속(재시작 생존).

mcstatus 미설치 시: ping()=None → "상태 조회 비활성" 표기(봇 기동엔 영향 없음).
"""
from __future__ import annotations

import logging

import discord
from discord import app_commands
from discord.ext import commands, tasks

from core import config, servers
from core.permissions import requires_permission
from integrations import mcping

log = logging.getLogger(__name__)

_REFRESH_MINUTES = 3


class ServerStatusCog(commands.Cog):
    def __init__(self, bot: commands.Bot) -> None:
        self.bot = bot

    @property
    def db(self):
        return self.bot.db  # type: ignore[attr-defined]

    async def cog_load(self) -> None:
        self.refresh.start()

    async def cog_unload(self) -> None:
        self.refresh.cancel()

    # ─── 주기 갱신 ────────────────────────────────────────────────
    @tasks.loop(minutes=_REFRESH_MINUTES)
    async def refresh(self) -> None:
        row = await servers.get_any_active(self.db)
        if row is None:
            return
        try:
            await self._update(row)
        except discord.HTTPException:
            log.warning("접속정보 갱신 실패: #%s", row["id"], exc_info=True)

    @refresh.before_loop
    async def _before(self) -> None:
        await self.bot.wait_until_ready()

    # ─── 게시/갱신 ────────────────────────────────────────────────
    async def _status_channel(
        self, guild: discord.Guild, row
    ) -> discord.TextChannel | None:
        """active 서버 정보 카테고리의 `접속정보` 텍스트 채널."""
        cats = await servers.get_categories(self.db, row["id"])
        info_id = next((c["category_id"] for c in cats if c["group_key"] == "info"), None)
        cat = guild.get_channel(info_id) if info_id else None
        if isinstance(cat, discord.CategoryChannel):
            for ch in cat.channels:
                if isinstance(ch, discord.TextChannel) and ch.name == "접속정보":
                    return ch
        return None

    async def _build(self, row) -> discord.Embed:
        addr = row["connect_address"]
        result = await mcping.ping(addr)
        embed = discord.Embed(
            title=f"{row['display_name']} 접속 정보", timestamp=discord.utils.utcnow()
        )
        embed.add_field(
            name="접속 주소", value=f"`{addr}`" if addr else "미설정 (`/서버주소`)", inline=False
        )
        if result is None:
            embed.add_field(
                name="상태",
                value="⚪ 주소 미설정" if mcping.available() else "⚪ 상태 조회 비활성(mcstatus)",
                inline=True,
            )
            embed.color = discord.Color.light_grey()
        elif result["online"]:
            embed.add_field(name="상태", value="🟢 온라인", inline=True)
            embed.add_field(name="인원", value=f"{result['players']}/{result['max']}", inline=True)
            embed.add_field(name="핑", value=f"{result['latency_ms']}ms", inline=True)
            embed.set_footer(text=f"버전 {result['version']} · {_REFRESH_MINUTES}분마다 갱신")
            embed.color = discord.Color.green()
        else:
            embed.add_field(name="상태", value="🔴 오프라인", inline=True)
            embed.set_footer(text=f"{_REFRESH_MINUTES}분마다 갱신")
            embed.color = discord.Color.red()
        return embed

    async def _update(self, row) -> None:
        guild = self.bot.get_guild(config.GUILD_ID)
        if guild is None:
            return
        channel = await self._status_channel(guild, row)
        if channel is None:
            return
        embed = await self._build(row)
        msg_id = row["status_message_id"]
        if msg_id:
            try:
                msg = await channel.fetch_message(msg_id)
                await msg.edit(embed=embed)
                return
            except discord.NotFound:
                pass  # 메시지 삭제됨 — 재게시
        sent = await channel.send(embed=embed)
        await servers.set_status_message_id(self.db, row["id"], sent.id)

    # ─── 수동 갱신 ────────────────────────────────────────────────
    @app_commands.command(name="접속정보", description="진행 중 서버의 접속정보를 즉시 갱신합니다.")
    @requires_permission("admin")
    async def refresh_now(self, interaction: discord.Interaction) -> None:
        row = await servers.get_any_active(self.db)
        if row is None:
            await interaction.response.send_message("진행 중(active)인 서버가 없습니다.", ephemeral=True)
            return
        await interaction.response.defer(ephemeral=True)
        await self._update(row)
        note = "" if row["connect_address"] else " (접속 주소 미설정 — `/서버주소` 로 설정)"
        await interaction.followup.send(f"접속정보 갱신 완료.{note}", ephemeral=True)


async def setup(bot: commands.Bot) -> None:
    await bot.add_cog(ServerStatusCog(bot))
