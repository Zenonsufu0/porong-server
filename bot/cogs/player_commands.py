"""
플레이어 정보 슬래시 명령어 (/프로필, /영지, /보스).
"""
from __future__ import annotations

import discord
from discord import app_commands
from discord.ext import commands

import config
from api_client import EmpireApiClient


def _embed_from_card(data: dict, color: discord.Color = discord.Color.blurple()) -> discord.Embed:
    """DiscordCardResponse JSON → discord.Embed 변환."""
    title: str = data.get("title", "정보")
    lines: list[str] = data.get("lines", [])
    footer: str = data.get("footer", "")
    embed = discord.Embed(title=title, description="\n".join(lines), color=color)
    if footer:
        embed.set_footer(text=footer)
    return embed


async def _resolve_nick(interaction: discord.Interaction, api: EmpireApiClient, nick: str | None) -> str | None:
    """닉네임 미입력 시 Discord 유저 ID로 연동된 닉네임을 반환."""
    if nick:
        return nick
    # 봇이 discord_user_id → minecraft_nick 을 직접 알 방법이 없으므로
    # /auth/status 는 nick→discord 방향이다. 여기서는 미입력 안내를 반환.
    return None


class PlayerCommandsCog(commands.Cog):
    def __init__(self, bot: commands.Bot) -> None:
        self.bot = bot
        self.api = EmpireApiClient()

    @app_commands.command(name="프로필", description="마인크래프트 플레이어 프로필을 조회합니다.")
    @app_commands.describe(닉네임="조회할 마인크래프트 닉네임 (미입력 시 안내)")
    async def profile(self, interaction: discord.Interaction, 닉네임: str | None = None) -> None:
        await self._player_info_command(interaction, 닉네임)

    @app_commands.command(name="영지", description="마인크래프트 영지 정보를 조회합니다.")
    @app_commands.describe(닉네임="조회할 마인크래프트 닉네임")
    async def territory(self, interaction: discord.Interaction, 닉네임: str | None = None) -> None:
        await self._player_info_command(interaction, 닉네임)

    @app_commands.command(name="보스", description="보스 처치 기록을 조회합니다.")
    @app_commands.describe(닉네임="조회할 마인크래프트 닉네임")
    async def boss(self, interaction: discord.Interaction, 닉네임: str | None = None) -> None:
        await self._player_info_command(interaction, 닉네임)

    async def _player_info_command(
        self, interaction: discord.Interaction, nick: str | None
    ) -> None:
        if not nick:
            await interaction.response.send_message(
                "닉네임을 입력해주세요. 예: `/프로필 Steve`", ephemeral=True
            )
            return

        await interaction.response.defer()
        try:
            data = await self.api.get_player_by_nick(nick)
        except Exception as e:
            await interaction.followup.send(f"조회 실패: {e}", ephemeral=True)
            return

        embed = _embed_from_card(data)
        await interaction.followup.send(embed=embed)

    async def cog_unload(self) -> None:
        await self.api.close()


async def setup(bot: commands.Bot) -> None:
    await bot.add_cog(PlayerCommandsCog(bot))
