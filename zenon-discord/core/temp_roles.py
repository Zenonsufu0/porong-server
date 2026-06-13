"""
임시역할 자동만료 데이터 접근 (T14) — community_level.md §10, data_model.md §2.8.

만료 시각(expires_at, epoch)이 있는 역할을 적재 → 주기 tick 이 만료분을 회수·삭제.
부여 경로 = 운영 명령(/임시역할부여) 또는 이벤트 모듈. 디스코드 측 상태(봇 DB 보관).
"""
from __future__ import annotations

from core.db import Database


async def grant(
    db: Database, user_id: int, role_id: int, expires_at: int, reason: str | None = None
) -> int:
    return await db.execute(
        "INSERT INTO temp_roles(discord_user_id, role_id, expires_at, reason) "
        "VALUES(?, ?, ?, ?)",
        (user_id, role_id, expires_at, reason),
    )


async def due(db: Database, now_epoch: int) -> list:
    """만료(expires_at <= now)된 행 목록. tick 회수 대상."""
    return await db.fetchall(
        "SELECT * FROM temp_roles WHERE expires_at <= ? ORDER BY expires_at", (now_epoch,)
    )


async def delete(db: Database, row_id: int) -> None:
    await db.execute("DELETE FROM temp_roles WHERE id = ?", (row_id,))


async def delete_user_role(db: Database, user_id: int, role_id: int) -> None:
    """수동 조기 회수 시 잔여 행 정리(중복 부여분 모두)."""
    await db.execute(
        "DELETE FROM temp_roles WHERE discord_user_id = ? AND role_id = ?",
        (user_id, role_id),
    )
