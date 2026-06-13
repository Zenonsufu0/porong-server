"""
warnings 경고 데이터 접근 (T15, data_model.md §2.4).

servers.py 와 같은 패턴 — 도메인/기능 모듈이 raw SQL 을 흩뿌리지 않도록
warnings 테이블 쿼리를 여기 중앙화한다. 쓰기는 core.db.Database 가 lock 으로 직렬화.

경고는 누적·보존 모델: 철회는 행 삭제가 아니라 active=0(이력 보존).
"""
from __future__ import annotations

from core.db import Database


async def add_warning(db: Database, *, user_id: int, reason: str, operator_id: int) -> int:
    """경고 추가 → 새 id 반환. created_at 은 DB측 시각(strftime)."""
    return await db.execute(
        "INSERT INTO warnings(discord_user_id, reason, operator_id, created_at, active) "
        "VALUES(?, ?, ?, strftime('%s','now'), 1)",
        (user_id, reason, operator_id),
    )


async def list_warnings(db: Database, user_id: int) -> list:
    """대상의 전체 경고 이력(활성+철회), 최신순."""
    return await db.fetchall(
        "SELECT * FROM warnings WHERE discord_user_id = ? ORDER BY created_at DESC, id DESC",
        (user_id,),
    )


async def count_active(db: Database, user_id: int) -> int:
    """대상의 활성 경고 수(에스컬레이션 안내용)."""
    row = await db.fetchone(
        "SELECT COUNT(*) AS n FROM warnings WHERE discord_user_id = ? AND active = 1",
        (user_id,),
    )
    return int(row["n"]) if row else 0


async def get_warning(db: Database, warning_id: int):
    return await db.fetchone("SELECT * FROM warnings WHERE id = ?", (warning_id,))


async def revoke_warning(db: Database, warning_id: int) -> bool:
    """경고 철회(active=1→0). 실제로 활성 행을 바꿨으면 True.

    이미 철회됐거나 없는 id 면 False(호출부에서 안내 분기).
    """
    cur = await db.fetchone(
        "SELECT active FROM warnings WHERE id = ?", (warning_id,)
    )
    if cur is None or int(cur["active"]) == 0:
        return False
    await db.execute("UPDATE warnings SET active = 0 WHERE id = ?", (warning_id,))
    return True
