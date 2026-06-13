"""
공통 명령어 모듈 — 특정 게임(RPG/포로몬)에 종속되지 않는 봇 전역 기능.

현재는 헬스체크용 /핑 만 제공한다. 서버 안내·도움말 등 공통 기능을
여기에 추가한다.
"""
from __future__ import annotations

import discord
from discord import app_commands
from discord.ext import commands


class CommonCog(commands.Cog):
    def __init__(self, bot: commands.Bot) -> None:
        self.bot = bot

    @app_commands.command(name="핑", description="봇 응답 지연(latency)을 확인합니다.")
    async def ping(self, interaction: discord.Interaction) -> None:
        latency_ms = round(self.bot.latency * 1000)
        await interaction.response.send_message(
            f"🏓 퐁! 지연: **{latency_ms}ms**", ephemeral=True
        )


async def setup(bot: commands.Bot) -> None:
    await bot.add_cog(CommonCog(bot))
