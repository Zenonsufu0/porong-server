"""
이벤트 명령어 모듈 — **스텁(구조만)**.

서버 공통 이벤트(일정 공지·이벤트 알림·참여 패널)가 들어갈 자리.
이벤트 관리 명령어는 권한이 필요하므로 core.permissions 의
requires_permission("event_manager") 등을 사용한다.

⚠️ 실제 명령어는 설계 확정 후 구현한다. 현재는 빈 Cog 다.

TODO (설계 확정 후):
  - /이벤트등록  : 이벤트 일정 등록·수정·삭제 (event_manager 권한)
  - /일정        : 예정 이벤트·보스 리젠 조회
  - 이벤트 시작/종료 알림 (이벤트알림 역할 멘션)
"""
from __future__ import annotations

from discord.ext import commands


class EventCog(commands.Cog):
    """이벤트 도메인 Cog (스텁)."""

    def __init__(self, bot: commands.Bot) -> None:
        self.bot = bot


async def setup(bot: commands.Bot) -> None:
    await bot.add_cog(EventCog(bot))
