"""
필드보스 상태 폴링 — RESPAWNING→ALIVE 전환 시 알림 채널에 embed 게시.
"""
from __future__ import annotations

import discord
from discord.ext import commands, tasks

import config
from api_client import PoroApiClient

# 필드 ID → 한글 표시명 매핑 (ExploreHubGui.Field 기준)
FIELD_NAMES: dict[str, str] = {
    "plain": "수도 외곽 평원",
    "mine": "폐광 지대",
    "sewer": "오염된 수로",
    "outpost": "무너진 초소",
    "ruins": "고대 성벽 잔해",
}


class FieldBossCog(commands.Cog):
    def __init__(self, bot: commands.Bot) -> None:
        self.bot = bot
        self.api = PoroApiClient()
        # field_id → 이전 상태 문자열 ("ALIVE" / "RESPAWNING" / "IMMINENT")
        self._prev_status: dict[str, str] = {}

    @commands.Cog.listener()
    async def on_ready(self) -> None:
        if not self.poll_field_boss.is_running():
            self.poll_field_boss.start()

    @tasks.loop(seconds=30)
    async def poll_field_boss(self) -> None:
        try:
            entries = await self.api.get_field_boss_status()
        except Exception:
            return  # 서버 오프라인 — 무시

        guild = self.bot.get_guild(config.GUILD_ID)
        if guild is None:
            return
        channel = guild.get_channel(config.CHANNEL_FIELD_BOSS_ID)
        if not isinstance(channel, discord.TextChannel):
            return

        notify_role_id = config.NOTIFY_ROLE_IDS.get("필드보스알림", 0)
        mention = f"<@&{notify_role_id}>" if notify_role_id else None

        for entry in entries:
            field_id: str = entry.get("field_id", "")
            status: str = entry.get("status", "")
            respawn_minutes: int = entry.get("respawn_minutes", 0) or 0
            display = FIELD_NAMES.get(field_id, field_id)

            # 0 < respawn_minutes <= 5 이면 IMMINENT로 취급
            effective = (
                "IMMINENT" if status == "RESPAWNING" and 0 < respawn_minutes <= 5
                else status
            )
            prev = self._prev_status.get(field_id)

            if prev is not None and prev != "ALIVE" and effective == "ALIVE":
                embed = discord.Embed(
                    title=f"⚠️ {display} 필드보스 등장!",
                    description=f"**{display}** 에 필드보스가 출현했습니다!",
                    color=discord.Color.red(),
                )
                embed.add_field(name="현재 접속자", value=str(entry.get("player_count", 0)), inline=True)
                try:
                    await channel.send(content=mention, embed=embed)
                except discord.HTTPException:
                    pass
            elif prev is not None and prev != "IMMINENT" and effective == "IMMINENT":
                embed = discord.Embed(
                    title=f"🕐 {display} 필드보스 5분 전!",
                    description=f"**{display}** 필드보스가 약 **{respawn_minutes}분** 후 출현합니다.",
                    color=discord.Color.orange(),
                )
                try:
                    await channel.send(content=mention, embed=embed)
                except discord.HTTPException:
                    pass

            self._prev_status[field_id] = effective

    @poll_field_boss.before_loop
    async def before_poll(self) -> None:
        await self.bot.wait_until_ready()

    async def cog_unload(self) -> None:
        self.poll_field_boss.cancel()
        await self.api.close()


async def setup(bot: commands.Bot) -> None:
    await bot.add_cog(FieldBossCog(bot))
