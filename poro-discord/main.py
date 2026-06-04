"""
포로 서버 디스코드 봇 엔트리포인트.
"""
import asyncio

import discord
from discord.ext import commands

from config import DISCORD_TOKEN

intents = discord.Intents.default()
intents.members = True

bot = commands.Bot(command_prefix="!", intents=intents)


@bot.event
async def on_ready() -> None:
    print(f"Poro Bot ready: {bot.user}")
    await bot.tree.sync()
    print("Slash commands synced.")


async def main() -> None:
    async with bot:
        await bot.load_extension("cogs.auth")
        await bot.load_extension("cogs.player_commands")
        await bot.load_extension("cogs.field_boss")
        await bot.load_extension("cogs.role_poll")
        await bot.load_extension("cogs.role_commands")
        await bot.start(DISCORD_TOKEN)


if __name__ == "__main__":
    asyncio.run(main())
