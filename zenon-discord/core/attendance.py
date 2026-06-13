"""
출석 / 일일보상 데이터 접근 (T14) — community_level.md §9, data_model.md §2.7.

하루 1회(KST 기준) `/출석` → streak(연속)·total(누적) 갱신 + XP 보상(community.add_xp).
날짜 경계 = KST(UTC+9). last_date 가 어제면 streak+1, 그 외(공백 포함)면 streak 리셋.
"""
from __future__ import annotations

from datetime import datetime, timedelta, timezone

from core.db import Database

_KST = timezone(timedelta(hours=9))


def _kst_dates() -> tuple[str, str]:
    """(오늘, 어제) KST 'YYYY-MM-DD' 문자열."""
    now = datetime.now(_KST)
    return now.strftime("%Y-%m-%d"), (now - timedelta(days=1)).strftime("%Y-%m-%d")


async def get_attendance(db: Database, user_id: int):
    return await db.fetchone(
        "SELECT * FROM attendance WHERE discord_user_id = ?", (user_id,)
    )


async def check_in(db: Database, user_id: int) -> dict:
    """출석 처리. 반환 dict:
      - already_checked: 오늘 이미 출석했는지
      - streak / total: 갱신 후 값
    XP 보상은 호출부(cog)가 streak 기반으로 community.add_xp 하여 분리."""
    today, yesterday = _kst_dates()
    row = await get_attendance(db, user_id)

    if row is not None and row["last_date"] == today:
        return {"already_checked": True, "streak": row["streak"], "total": row["total"]}

    if row is not None and row["last_date"] == yesterday:
        streak = int(row["streak"]) + 1
    else:
        streak = 1
    total = (int(row["total"]) if row is not None else 0) + 1

    await db.execute(
        "INSERT INTO attendance(discord_user_id, last_date, streak, total) "
        "VALUES(?, ?, ?, ?) "
        "ON CONFLICT(discord_user_id) DO UPDATE SET "
        "  last_date = excluded.last_date, streak = excluded.streak, total = excluded.total",
        (user_id, today, streak, total),
    )
    return {"already_checked": False, "streak": streak, "total": total}
