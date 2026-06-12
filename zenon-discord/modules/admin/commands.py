"""
관리자/운영 명령어 모듈 — **스텁(구조만)**.

운영진 전용 명령어(공지·점검·역할 부여·골드 지급·유저 조회 등)가 들어갈 자리.

⚠️ 보안 영향이 큰 영역이다. 실제 구현 전 설계 문서/인터페이스부터 확정한다.
  - 모든 운영 명령어는 core.permissions.requires_permission(...) 로 보호한다.
  - 운영 권한 역할은 수동 지급 전용이다(봇 자동 지급 금지).
  - 골드 지급·역할 부여 등 상태 변경은 RPG/포로몬 서버 API 경유로만,
    그리고 사용자가 명시적으로 요청할 때만 구현한다.

현재는 로드만 되는 빈 Cog 다(등록 명령어 없음).

TODO (설계/승인 후):
  - /공지        : 공식 공지 게시 (admin)
  - /점검        : 점검 안내 + 점검알림 멘션 (admin)
  - /역할부여    : 수동 역할 부여 (admin) — 권한 역할 부여는 운영진 한정
  - /유저조회    : 플레이어 상세 조회 (admin, ephemeral)
  - /골드지급    : 골드 지급 (rpg_manager, API 경유 + 운영로그 기록)
"""
from __future__ import annotations

from discord.ext import commands


class AdminCog(commands.Cog):
    """관리자 도메인 Cog (스텁)."""

    def __init__(self, bot: commands.Bot) -> None:
        self.bot = bot


async def setup(bot: commands.Bot) -> None:
    await bot.add_cog(AdminCog(bot))
