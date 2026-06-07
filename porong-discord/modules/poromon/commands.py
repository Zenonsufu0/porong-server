"""
포로몬 명령어 모듈 — **스텁(구조만)**.

포로몬(모드 서버) 전용 조회/알림 명령어가 들어갈 자리.
RPG 모듈과 완전히 분리한다. 실제 포로몬 서버 연동은
integrations/poromon_api.py(스텁)를 통해서만 한다.

⚠️ 실제 명령어/연동은 사용자가 명시적으로 요청할 때 구현한다.
현재는 로드만 되는 빈 Cog 다(등록 명령어 없음).

TODO (설계 확정 후):
  - /포로몬현황  : 모드 서버 상태(접속 인원·TPS) 조회
  - /포로몬도감  : 포로몬 도감/보유 현황 조회
  - 포로몬 이벤트 알림 (포로몬알림 역할 멘션)
"""
from __future__ import annotations

from discord.ext import commands


class PoromonCog(commands.Cog):
    """포로몬 도메인 Cog (스텁)."""

    def __init__(self, bot: commands.Bot) -> None:
        self.bot = bot


async def setup(bot: commands.Bot) -> None:
    await bot.add_cog(PoromonCog(bot))
