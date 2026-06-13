"""
출석 / 일일보상 — T14 community_level.md §9.

`/출석` → 하루 1회(KST) 체크 → 연속 streak·누적 total 갱신 + XP 보상(레벨 연동).
보상 = `ATTENDANCE_XP_BASE + min(streak, CAP) * ATTENDANCE_XP_PER_STREAK`(config).
레벨업/칭호 처리는 CommunityLevelCog 에 위임(중복 로직 방지).
"""
from __future__ import annotations

import logging

import discord
from discord import app_commands
from discord.ext import commands

from core import attendance, community, config

log = logging.getLogger(__name__)


def _reward_xp(streak: int) -> int:
    capped = min(streak, config.ATTENDANCE_STREAK_CAP)
    return config.ATTENDANCE_XP_BASE + capped * config.ATTENDANCE_XP_PER_STREAK


class AttendanceCog(commands.Cog):
    def __init__(self, bot: commands.Bot) -> None:
        self.bot = bot

    @property
    def db(self):
        return self.bot.db  # type: ignore[attr-defined]

    @app_commands.command(name="출석", description="하루 1회 출석하고 보상 XP를 받습니다.")
    async def attend(self, interaction: discord.Interaction) -> None:
        result = await attendance.check_in(self.db, interaction.user.id)

        if result["already_checked"]:
            await interaction.response.send_message(
                f"오늘은 이미 출석했습니다. (연속 {result['streak']}일 · 누적 {result['total']}일)",
                ephemeral=True,
            )
            return

        streak, total = result["streak"], result["total"]
        reward = _reward_xp(streak)
        _, new_level, leveled_up = await community.add_xp(self.db, interaction.user.id, reward)

        embed = discord.Embed(
            title="📅 출석 완료!",
            description=f"{interaction.user.mention} 님, 출석 보상 **+{reward} XP**",
            color=discord.Color.green(),
        )
        embed.add_field(name="연속 출석", value=f"🔥 {streak}일", inline=True)
        embed.add_field(name="누적 출석", value=f"{total}일", inline=True)
        await interaction.response.send_message(embed=embed)

        if leveled_up:
            level_cog = self.bot.get_cog("CommunityLevelCog")
            if level_cog is not None and interaction.guild is not None:
                await level_cog._handle_levelup(  # type: ignore[attr-defined]
                    interaction.guild, interaction.user, new_level
                )


async def setup(bot: commands.Bot) -> None:
    await bot.add_cog(AttendanceCog(bot))
