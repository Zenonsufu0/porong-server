"""
Zenon Mon 명령어 모듈 — **스텁(구조만)**.

Zenon Mon(모드 서버) 전용 조회/알림 명령어가 들어갈 자리.
RPG 모듈과 완전히 분리한다. 실제 Zenon Mon 서버 연동은
integrations/zenon_mon_api.py 를 통해서만 한다.

⚠️ 인증 흐름(약관 게이트 + 인증 버튼/모달)은 도메인 비종속 공통 온보딩
(`modules/onboarding/panels.py`)이 담당한다. Zenon Mon verify 는 그쪽에서
integrations/zenon_mon_api.py 의 verify_code 를 호출한다. 이 모듈은 Zenon Mon
*고유* 조회/알림 명령만 다룬다.

TODO (설계 확정 후):
  - /포로몬현황  : 모드 서버 상태(접속 인원·TPS) 조회
  - /포로몬도감  : Zenon Mon 도감/보유 현황 조회
  - Zenon Mon 이벤트 알림 (포로몬알림 역할 멘션)
"""
from __future__ import annotations

from discord.ext import commands


class ZenonMonCog(commands.Cog):
    """Zenon Mon 도메인 Cog (스텁)."""

    def __init__(self, bot: commands.Bot) -> None:
        self.bot = bot


async def setup(bot: commands.Bot) -> None:
    await bot.add_cog(ZenonMonCog(bot))
