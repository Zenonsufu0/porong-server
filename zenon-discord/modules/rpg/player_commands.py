"""
플레이어 정보 슬래시 명령어 (/프로필, /영지, /보스).
"""
from __future__ import annotations

import discord
from discord import app_commands
from discord.ext import commands

from integrations.rpg_api import PoroApiClient


def _embed_from_card(data: dict, color: discord.Color = discord.Color.blurple()) -> discord.Embed:
    """DiscordCardResponse JSON → discord.Embed 변환."""
    title: str = data.get("title", "정보")
    lines: list[str] = data.get("lines", [])
    footer: str = data.get("footer", "")
    embed = discord.Embed(title=title, description="\n".join(lines), color=color)
    if footer:
        embed.set_footer(text=footer)
    return embed


class PlayerCommandsCog(commands.Cog):
    def __init__(self, bot: commands.Bot) -> None:
        self.bot = bot
        self.api = PoroApiClient()

    @app_commands.command(name="프로필", description="마인크래프트 플레이어 프로필을 조회합니다.")
    @app_commands.describe(닉네임="조회할 마인크래프트 닉네임")
    async def profile(self, interaction: discord.Interaction, 닉네임: str | None = None) -> None:
        await self._info_command(interaction, 닉네임, self.api.get_player_by_nick)

    @app_commands.command(name="영지", description="마인크래프트 영지 정보를 조회합니다.")
    @app_commands.describe(닉네임="조회할 마인크래프트 닉네임")
    async def territory(self, interaction: discord.Interaction, 닉네임: str | None = None) -> None:
        await self._info_command(interaction, 닉네임, self.api.get_island_by_nick)

    @app_commands.command(name="보스", description="보스 처치 기록을 조회합니다.")
    @app_commands.describe(닉네임="조회할 마인크래프트 닉네임")
    async def boss(self, interaction: discord.Interaction, 닉네임: str | None = None) -> None:
        await self._info_command(interaction, 닉네임, self.api.get_boss_by_nick)

    async def _info_command(self, interaction: discord.Interaction, nick: str | None, api_fn) -> None:
        if not nick:
            await interaction.response.send_message(
                "닉네임을 입력해주세요. 예: `/프로필 Steve`", ephemeral=True
            )
            return
        await interaction.response.defer()
        try:
            data = await api_fn(nick)
        except Exception as e:
            await interaction.followup.send(f"조회 실패: {e}", ephemeral=True)
            return
        embed = _embed_from_card(data)
        await interaction.followup.send(embed=embed)

    async def cog_unload(self) -> None:
        await self.api.close()


async def setup(bot: commands.Bot) -> None:
    await bot.add_cog(PlayerCommandsCog(bot))
