"""
커뮤니티 레벨 데이터 접근 + 레벨 곡선 (T13) — community_level.md.

XP → 레벨 곡선은 순수 함수(§12.4 — discord 없이 단위 테스트). XP 쓰기는
core.db.Database 가 lock 으로 직렬화. 유저 식별 = discord_user_id(길드 전역).
"""
from __future__ import annotations

from core.db import Database


# ─── 레벨 곡선 (순수 함수) ────────────────────────────────────────────

def total_xp_for_level(level: int) -> int:
    """레벨 `level` 에 도달하는 데 필요한 누적 XP(점증 곡선, community_level §2).

    레벨 0 = 0 XP(시작). L>=1 = 5*L^2 + 50*L + 100.
    """
    if level <= 0:
        return 0
    return 5 * level * level + 50 * level + 100


def level_for_xp(xp: int) -> int:
    """누적 XP 에 해당하는 레벨(total_xp_for_level 의 역함수, 단조 증가)."""
    level = 0
    while total_xp_for_level(level + 1) <= xp:
        level += 1
    return level


# ─── 접근 계층 ────────────────────────────────────────────────────────

async def get_xp(db: Database, user_id: int):
    return await db.fetchone(
        "SELECT * FROM community_xp WHERE discord_user_id = ?", (user_id,)
    )


async def add_xp(
    db: Database,
    user_id: int,
    amount: int,
    *,
    set_last_message_ts: int | None = None,
    add_voice_seconds: int = 0,
) -> tuple[int, int, bool]:
    """XP 가산 + 레벨 재계산(upsert). 반환 = (new_xp, new_level, leveled_up).

    leveled_up = 이 가산으로 레벨이 올랐는지(레벨업 알림 트리거용).
    """
    row = await get_xp(db, user_id)
    old_xp = int(row["xp"]) if row else 0
    old_level = int(row["level"]) if row else 0
    new_xp = max(0, old_xp + amount)
    new_level = level_for_xp(new_xp)
    last_ts = set_last_message_ts if set_last_message_ts is not None else (
        int(row["last_message_ts"]) if row else 0
    )
    await db.execute(
        "INSERT INTO community_xp"
        "(discord_user_id, xp, level, last_message_ts, voice_seconds, updated_at) "
        "VALUES(?, ?, ?, ?, ?, strftime('%s','now')) "
        "ON CONFLICT(discord_user_id) DO UPDATE SET "
        "  xp = excluded.xp, level = excluded.level, "
        "  last_message_ts = excluded.last_message_ts, "
        "  voice_seconds = community_xp.voice_seconds + ?, "
        "  updated_at = excluded.updated_at",
        (user_id, new_xp, new_level, last_ts, add_voice_seconds, add_voice_seconds),
    )
    return new_xp, new_level, new_level > old_level


async def top(db: Database, limit: int = 10) -> list:
    return await db.fetchall(
        "SELECT discord_user_id, xp, level FROM community_xp "
        "ORDER BY xp DESC LIMIT ?",
        (limit,),
    )


async def rank_of(db: Database, user_id: int) -> int | None:
    """유저의 1-기반 순위(XP 내림차순). 행 없으면 None."""
    row = await get_xp(db, user_id)
    if row is None:
        return None
    higher = await db.fetchone(
        "SELECT COUNT(*) AS n FROM community_xp WHERE xp > ?", (int(row["xp"]),)
    )
    return int(higher["n"]) + 1 if higher else None
