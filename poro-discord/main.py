"""
포로 서버 중앙제어 디스코드 봇 엔트리포인트.

봇은 포로서버 전체(공통·RPG·포로몬·이벤트·관리자·역할)의 운영 허브다.
실제 게임 로직은 RPG 플러그인 / 포로몬 서버가 담당하고, 봇은
명령어·권한·알림·조회·운영자 패널만 담당한다.

확장(extension)은 모듈 네임스페이스 단위로 로드한다:
  modules.<도메인>.<cog>
"""
import asyncio

import discord
from discord.ext import commands

from core.config import DISCORD_TOKEN

# ─── 로드할 확장 목록 (모듈 네임스페이스 단위) ──────────────────────
# 활성: 실제 구현·동작하는 모듈
# 스텁: 구조만 잡힌 모듈 (TODO 인터페이스 — 명령어 없음). 설계 확정 후 채운다.
EXTENSIONS: list[str] = [
    # 공통
    "modules.common.general",
    # RPG
    "modules.rpg.auth",
    "modules.rpg.player_commands",
    "modules.rpg.field_boss",
    "modules.rpg.role_poll",
    # 역할(권한·알림 분리)
    "modules.roles.role_commands",
    # ── 아래는 스텁: 설계/인터페이스 단계. 구현 전까지 명령어 없음 ──
    "modules.poromon.commands",
    "modules.event.commands",
    "modules.admin.commands",
]

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
        for ext in EXTENSIONS:
            await bot.load_extension(ext)
        await bot.start(DISCORD_TOKEN)


if __name__ == "__main__":
    asyncio.run(main())
