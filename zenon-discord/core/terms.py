"""
서버별 약관 저장/조회 (T7 온보딩) — data_model.md `terms`(v5).

약관은 서버(도메인)마다 다르고 시즌마다 바뀔 수 있어 **코드 하드코딩 대신 DB 저장**한다.
운영자가 `/약관설정`(모달)으로 입력/수정 → 온보딩 약관 패널이 이 내용을 게시한다.
키 = domain(온보딩 레지스트리와 정합, domain당 active 1 전제).
"""
from __future__ import annotations

from core.db import Database


async def get_terms(db: Database, domain: str) -> str | None:
    row = await db.fetchone("SELECT content FROM terms WHERE domain = ?", (domain,))
    return row["content"] if row else None


async def set_terms(db: Database, domain: str, content: str, operator_id: int) -> None:
    """약관 upsert(도메인별 1건). updated_at = DB측 시각."""
    await db.execute(
        "INSERT INTO terms(domain, content, updated_by, updated_at) "
        "VALUES(?, ?, ?, strftime('%s','now')) "
        "ON CONFLICT(domain) DO UPDATE SET "
        "content = excluded.content, updated_by = excluded.updated_by, updated_at = excluded.updated_at",
        (domain, content, operator_id),
    )
