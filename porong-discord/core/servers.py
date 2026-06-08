"""
servers 레지스트리 데이터 접근 (T21, data_model.md §2.1).

도메인 모듈이 raw SQL 을 흩뿌리지 않도록 servers 테이블 쿼리를 여기 중앙화한다.
각 함수는 core.db.Database 를 받아 호출한다(쓰기는 Database 가 lock 으로 직렬화).
"""
from __future__ import annotations

import time

from core.db import Database

VALID_STATES: tuple[str, ...] = ("prep", "active", "ended")


async def list_servers(db: Database) -> list:
    return await db.fetchall(
        "SELECT * FROM servers ORDER BY domain, season_no"
    )


async def get_server(db: Database, server_id: int):
    return await db.fetchone("SELECT * FROM servers WHERE id = ?", (server_id,))


async def get_by_domain_season(db: Database, domain: str, season_no: int):
    return await db.fetchone(
        "SELECT * FROM servers WHERE domain = ? AND season_no = ?",
        (domain, season_no),
    )


async def get_active(db: Database, domain: str):
    """domain 의 active 시즌 행(없으면 None). 게이팅 3층 ③에서 사용."""
    return await db.fetchone(
        "SELECT * FROM servers WHERE domain = ? AND state = 'active'",
        (domain,),
    )


async def create_prep_server(
    db: Database,
    domain: str,
    season_no: int,
    display_name: str,
    *,
    category_id: int | None = None,
    access_role_id: int | None = None,
    api_env_key: str | None = None,
) -> int:
    """prep 상태 서버 행 생성 → 새 id 반환.

    UNIQUE(domain, season_no) 위반 시 sqlite3.IntegrityError 전파(호출부에서 처리).
    """
    now = int(time.time())
    return await db.execute(
        "INSERT INTO servers"
        "(domain, season_no, display_name, state, category_id, access_role_id, api_env_key, created_at) "
        "VALUES(?, ?, ?, 'prep', ?, ?, ?, ?)",
        (domain, season_no, display_name, category_id, access_role_id, api_env_key, now),
    )


async def set_state(db: Database, server_id: int, new_state: str, *, ended_at: int | None = None) -> None:
    """서버 상태 전이. 전이 유효성(prep→active→ended)은 호출부가 검증한다.

    domain당 active 1개 부분 유니크 인덱스에 의해, 같은 domain에 active가 이미 있으면
    prep→active UPDATE 시 sqlite3.IntegrityError 가 전파된다(호출부에서 처리).
    """
    if new_state not in VALID_STATES:
        raise ValueError(f"invalid state: {new_state}")
    await db.execute(
        "UPDATE servers SET state = ?, ended_at = ? WHERE id = ?",
        (new_state, ended_at, server_id),
    )
