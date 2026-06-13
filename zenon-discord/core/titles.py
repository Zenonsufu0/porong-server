"""
칭호 데이터 접근 (T13) — community_level.md §3.2, data_model.md §2.3·2.3b.

칭호 = 디스코드 역할이 **아님**(순수 표시 데이터/코스메틱). 레벨 임계(`required_level`)
도달 시 보유 추가(누적, 회수 안 함), 유저당 장착 1개(`/칭호`로 교체).
"""
from __future__ import annotations

from core.db import Database


async def list_catalog(db: Database) -> list:
    return await db.fetchall("SELECT * FROM titles ORDER BY required_level, id")


async def owned_titles(db: Database, user_id: int) -> list:
    """보유 칭호(장착 여부 포함), 임계 낮은 순."""
    return await db.fetchall(
        "SELECT t.id, t.display_name, t.required_level, ut.equipped "
        "FROM user_titles ut JOIN titles t ON t.id = ut.title_id "
        "WHERE ut.discord_user_id = ? ORDER BY t.required_level, t.id",
        (user_id,),
    )


async def equipped_title(db: Database, user_id: int) -> str | None:
    """장착 중인 칭호 display_name(없으면 None)."""
    row = await db.fetchone(
        "SELECT t.display_name FROM user_titles ut JOIN titles t ON t.id = ut.title_id "
        "WHERE ut.discord_user_id = ? AND ut.equipped = 1",
        (user_id,),
    )
    return row["display_name"] if row else None


async def grant_title(db: Database, user_id: int, title_id: int) -> None:
    """보유 추가(이미 있으면 무시)."""
    await db.execute(
        "INSERT OR IGNORE INTO user_titles(discord_user_id, title_id, acquired_at, equipped) "
        "VALUES(?, ?, strftime('%s','now'), 0)",
        (user_id, title_id),
    )


async def newly_eligible(db: Database, user_id: int, level: int) -> list:
    """레벨 도달로 새로 받을 수 있는(아직 미보유) 칭호들."""
    return await db.fetchall(
        "SELECT * FROM titles WHERE required_level IS NOT NULL AND required_level <= ? "
        "AND id NOT IN (SELECT title_id FROM user_titles WHERE discord_user_id = ?) "
        "ORDER BY required_level",
        (level, user_id),
    )


async def equip(db: Database, user_id: int, title_id: int) -> bool:
    """보유 칭호 1개 장착(나머지 해제). 미보유면 False."""
    owned = await db.fetchone(
        "SELECT 1 FROM user_titles WHERE discord_user_id = ? AND title_id = ?",
        (user_id, title_id),
    )
    if owned is None:
        return False
    await db.execute(
        "UPDATE user_titles SET equipped = 0 WHERE discord_user_id = ?", (user_id,)
    )
    await db.execute(
        "UPDATE user_titles SET equipped = 1 WHERE discord_user_id = ? AND title_id = ?",
        (user_id, title_id),
    )
    return True
