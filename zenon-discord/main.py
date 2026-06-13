"""
포롱 서버 중앙제어 디스코드 봇 엔트리포인트.

봇은 포롱서버 전체(공통·RPG·포로몬·이벤트·관리자·역할)의 운영 허브다.
실제 게임 로직은 RPG 플러그인 / 포로몬 서버가 담당하고, 봇은
명령어·권한·알림·조회·운영자 패널만 담당한다.

확장(extension)은 모듈 네임스페이스 단위로 로드한다:
  modules.<도메인>.<cog>
"""
import asyncio
import logging

import discord
from discord import app_commands
from discord.ext import commands

from core import config
from core.config import DISCORD_TOKEN
from core.db import Database

log = logging.getLogger(__name__)

# ─── 로드할 확장 목록 (모듈 네임스페이스 단위) ──────────────────────
# 활성: 실제 구현·동작하는 모듈
# 스텁: 구조만 잡힌 모듈 (TODO 인터페이스 — 명령어 없음). 설계 확정 후 채운다.
EXTENSIONS: list[str] = [
    # 공통
    "modules.common.general",
    "modules.common.server_status",
    # 통합 알림 embed 빌더 등록 (T1)
    "modules.notify.catalog",
    # 커뮤니티 (T13·T14)
    "modules.community.temp_voice",
    "modules.community.level",
    "modules.community.attendance",
    "modules.community.temp_roles",
    # 지원 (T16)
    "modules.support.tickets",
    "modules.support.faq",
    "modules.support.bug_report",
    # RPG (auth·role_poll = 구방향 폐기, DL-138 — 온보딩은 modules.onboarding 으로 통일)
    "modules.rpg.player_commands",
    "modules.rpg.field_boss",
    # 역할(권한·알림 분리)
    "modules.roles.role_commands",
    # 공통 온보딩(약관 게이트 + 인증 버튼/모달 패널)
    "modules.onboarding.panels",
    # 서버 레지스트리 / 생애주기 (T21)
    "modules.server_lifecycle.commands",
    # 모더레이션 — 경고계 (T15)
    "modules.moderation.commands",
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
    print(f"Porong Bot ready: {bot.user}")
    await bot.tree.sync()
    print("Slash commands synced.")


@bot.tree.error
async def on_app_command_error(
    interaction: discord.Interaction, error: app_commands.AppCommandError
) -> None:
    """전역 app command 에러 핸들러.

    권한/체크 실패(requires_permission 등)는 사유를 ephemeral 로 안내하고,
    그 외 예외는 일반 오류 안내 + 로깅한다(거부가 조용히 묻히지 않도록).
    """
    if isinstance(error, app_commands.CheckFailure):
        msg = str(error) or "이 명령어를 실행할 권한이 없습니다."
    else:
        log.exception("app command 오류: %s", error)
        msg = "명령 처리 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요."
    try:
        if interaction.response.is_done():
            await interaction.followup.send(msg, ephemeral=True)
        else:
            await interaction.response.send_message(msg, ephemeral=True)
    except discord.HTTPException:
        pass


async def main() -> None:
    # SQLite 접근 계층(T12) 기동 — 확장 로드 전에 연결해 Cog 들이 bot.db 사용 가능.
    db = Database(config.BOT_DB_PATH)
    await db.connect()
    bot.db = db  # type: ignore[attr-defined]

    inbound = None
    try:
        async with bot:
            for ext in EXTENSIONS:
                await bot.load_extension(ext)
            # 인바운드 알림 리스너(T1) — SECRET·PORT 설정 시에만 기동(무인증 방지).
            from core import inbound as inbound_mod
            if inbound_mod.is_enabled():
                inbound = inbound_mod.InboundServer(bot)
                await inbound.start()
            else:
                log.info("인바운드 알림 리스너 비활성(INBOUND_SECRET/PORT 미설정)")
            await bot.start(DISCORD_TOKEN)
    finally:
        if inbound is not None:
            await inbound.cleanup()
        await db.close()


if __name__ == "__main__":
    asyncio.run(main())
