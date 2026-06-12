"""
FAQ(자주 묻는 질문) 데이터 접근 (T16) — support.md §3, data_model.md §2.9.

FAQ 내용은 운영자가 `/FAQ추가`로 직접 등록한다(LLM 미사용 — §9.2). 봇은 저장소·조회 패널만 제공.
domain NULL = 공통(전 서버), 값 = 서버별. 조회 패널은 공통 + 현재 active 도메인만 노출.
"""
from __future__ import annotations

from core.db import Database


async def list_for_domain(db: Database, domain: str | None) -> list:
    """공통(NULL) + 지정 도메인 FAQ. domain=None 이면 공통만. 패널 노출용."""
    if domain is None:
        return await db.fetchall(
            "SELECT * FROM faq WHERE domain IS NULL ORDER BY id"
        )
    return await db.fetchall(
        "SELECT * FROM faq WHERE domain IS NULL OR domain = ? ORDER BY domain IS NULL DESC, id",
        (domain,),
    )


async def list_all(db: Database) -> list:
    """전체 FAQ(운영 관리·자동완성용)."""
    return await db.fetchall("SELECT * FROM faq ORDER BY domain IS NULL DESC, domain, id")


async def get_faq(db: Database, faq_id: int):
    return await db.fetchone("SELECT * FROM faq WHERE id = ?", (faq_id,))


async def add_faq(
    db: Database, domain: str | None, trigger: str, answer: str, operator_id: int
) -> int:
    return await db.execute(
        "INSERT INTO faq(domain, trigger, answer, updated_by, updated_at) "
        "VALUES(?, ?, ?, ?, strftime('%s','now'))",
        (domain, trigger, answer, operator_id),
    )


async def update_faq(
    db: Database, faq_id: int, trigger: str, answer: str, operator_id: int
) -> None:
    await db.execute(
        "UPDATE faq SET trigger = ?, answer = ?, updated_by = ?, "
        "updated_at = strftime('%s','now') WHERE id = ?",
        (trigger, answer, operator_id, faq_id),
    )


async def delete_faq(db: Database, faq_id: int) -> None:
    await db.execute("DELETE FROM faq WHERE id = ?", (faq_id,))
