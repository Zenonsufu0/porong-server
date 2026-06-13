"""
티켓(1:1 문의) 데이터 접근 (T16) — support.md §2, data_model.md §2.6.

비공개 채널 = 디스코드 측 상태 → 봇 DB 에 채널/상태 보관(게임 무관).
"""
from __future__ import annotations

from core.db import Database


async def create_ticket(
    db: Database, channel_id: int, opener_id: int, domain: str | None = None
) -> int:
    return await db.execute(
        "INSERT INTO tickets(channel_id, opener_id, domain, state, created_at) "
        "VALUES(?, ?, ?, 'open', strftime('%s','now'))",
        (channel_id, opener_id, domain),
    )


async def get_open_by_opener(db: Database, opener_id: int):
    """유저의 열린 티켓(동시 1개 제한 판정용). 없으면 None."""
    return await db.fetchone(
        "SELECT * FROM tickets WHERE opener_id = ? AND state = 'open' LIMIT 1",
        (opener_id,),
    )


async def get_by_channel(db: Database, channel_id: int):
    return await db.fetchone(
        "SELECT * FROM tickets WHERE channel_id = ?", (channel_id,)
    )


async def close_ticket(db: Database, ticket_id: int, closed_by: int) -> None:
    await db.execute(
        "UPDATE tickets SET state = 'closed', closed_at = strftime('%s','now'), closed_by = ? "
        "WHERE id = ?",
        (closed_by, ticket_id),
    )
