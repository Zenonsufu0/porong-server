"""
봇 SQLite 접근 계층 (T12) — data_model.md 의 구현.

원칙(data_model.md §0):
  - 봇 DB = **디스코드 측 상태만**. 게임 상태(프로필·영지·보스·화이트리스트·인증)와
    마크닉↔디스코드 매핑은 저장하지 않는다(권위=게임서버, 필요 시 HTTP 조회).
  - 유저 식별 = `discord_user_id`(정수). 길드 1개 전제 → guild_id 컬럼 생략.

런타임(§1):
  - aiosqlite(비동기) 단일 커넥션. 동기 sqlite3 금지(이벤트 루프 블로킹).
  - 쓰기는 lock 으로 직렬화(§12.2 — on_message XP·voice tick·명령 동시 쓰기 방어).
  - 도메인 모듈은 이 계층 경유로만 접근(raw SQL 분산 금지).

마이그레이션(§1): `schema_meta.version` 기반 단순 증분 러너.
  - `_MIGRATIONS` 는 **append-only**. 인덱스 i 적용 후 version = i+1.
  - 기존 마이그레이션 SQL 을 수정하지 말 것(이미 적용된 DB 와 어긋남) — 새 항목 추가만.
"""
from __future__ import annotations

import asyncio
import logging

import aiosqlite

log = logging.getLogger(__name__)

# 증분 마이그레이션 (append-only). 각 원소 = 한 버전에서 실행할 SQL 스크립트.
_MIGRATIONS: list[str] = [
    # v1 — servers 레지스트리 / 생애주기 (data_model.md §2.1, T21)
    """
    CREATE TABLE IF NOT EXISTS servers (
        id             INTEGER PRIMARY KEY AUTOINCREMENT,
        domain         TEXT    NOT NULL,
        season_no      INTEGER NOT NULL,
        display_name   TEXT    NOT NULL,
        state          TEXT    NOT NULL CHECK(state IN ('prep','active','ended')),
        category_id    INTEGER,
        access_role_id INTEGER,
        api_env_key    TEXT,
        created_at     INTEGER NOT NULL,
        ended_at       INTEGER,
        UNIQUE(domain, season_no)
    );
    -- domain 당 active 최대 1개 강제(§2.1 — 게이팅 단수 가정)
    CREATE UNIQUE INDEX IF NOT EXISTS idx_servers_active_per_domain
        ON servers(domain) WHERE state = 'active';
    """,
    # v2 — mod_log 운영/감사 로그 (data_model.md §2.5, T15)
    # 모든 제재·서버 생애주기·운영명령의 단일 적재처. #운영로그 게시는 core/mod_log.py.
    """
    CREATE TABLE IF NOT EXISTS mod_log (
        id          INTEGER PRIMARY KEY AUTOINCREMENT,
        action      TEXT    NOT NULL,
        target_id   INTEGER,
        operator_id INTEGER NOT NULL,
        reason      TEXT,
        detail      TEXT,
        created_at  INTEGER NOT NULL
    );
    CREATE INDEX IF NOT EXISTS idx_mod_log_target  ON mod_log(target_id);
    CREATE INDEX IF NOT EXISTS idx_mod_log_action  ON mod_log(action);
    CREATE INDEX IF NOT EXISTS idx_mod_log_created ON mod_log(created_at);
    """,
    # v3 — warnings 경고 (data_model.md §2.4, T15)
    # 디스코드 측 수동 경고. active=0 = 철회(이력 보존, 삭제 안 함).
    """
    CREATE TABLE IF NOT EXISTS warnings (
        id              INTEGER PRIMARY KEY AUTOINCREMENT,
        discord_user_id INTEGER NOT NULL,
        reason          TEXT,
        operator_id     INTEGER NOT NULL,
        created_at      INTEGER NOT NULL,
        active          INTEGER NOT NULL DEFAULT 1
    );
    CREATE INDEX IF NOT EXISTS idx_warnings_user ON warnings(discord_user_id);
    """,
    # v4 — 서버 다중 카테고리 + 온보딩 3역할 (T17 프리픽스 카테고리 그룹)
    # servers.category_id/access_role_id(단수, 수동연결 경로 유지)에 더해:
    #   pending_role_id·player_role_id = 온보딩 3역할(접근=access_role_id / 인증전 / 플레이어)
    #   server_categories = 자동전개 다중 카테고리(group_key 별 1개). 게이팅은 집합 매칭.
    """
    ALTER TABLE servers ADD COLUMN pending_role_id INTEGER;
    ALTER TABLE servers ADD COLUMN player_role_id  INTEGER;
    CREATE TABLE IF NOT EXISTS server_categories (
        server_id   INTEGER NOT NULL,
        group_key   TEXT    NOT NULL,
        category_id INTEGER NOT NULL,
        PRIMARY KEY (server_id, group_key)
    );
    CREATE INDEX IF NOT EXISTS idx_server_categories_cat ON server_categories(category_id);
    """,
    # v5 — terms 서버별 약관 (온보딩 약관 채널 게시용). 하드코딩 대신 봇 입력/DB 저장.
    """
    CREATE TABLE IF NOT EXISTS terms (
        domain     TEXT PRIMARY KEY,
        content    TEXT    NOT NULL,
        updated_by INTEGER,
        updated_at INTEGER NOT NULL
    );
    """,
    # v6 — 전역 active 서버 1개 강제 (2026-06-09, task.md §5)
    # RPG·포로몬 통틀어 active 는 최대 1행. state='active' 인 행끼리 state(상수) 유니크.
    # 기존 per-domain 인덱스(v1)는 이 전역 인덱스에 포섭(중복이나 무해).
    """
    CREATE UNIQUE INDEX IF NOT EXISTS idx_servers_single_active
        ON servers(state) WHERE state = 'active';
    """,
    # v7 — 접속정보(T18): 마인크래프트 접속 주소 + 상태 임베드 메시지 ID
    # connect_address = 플레이어 접속 host[:port](SLP 핑 대상). status_message_id = 갱신용.
    """
    ALTER TABLE servers ADD COLUMN connect_address   TEXT;
    ALTER TABLE servers ADD COLUMN status_message_id INTEGER;
    """,
    # v8 — community_xp 커뮤니티 레벨 (data_model.md §2.2, T13)
    # 디스코드 활동(채팅·음성) XP → 레벨. 길드 전역(도메인 무관). 메시지 내용 미열람.
    """
    CREATE TABLE IF NOT EXISTS community_xp (
        discord_user_id INTEGER PRIMARY KEY,
        xp              INTEGER NOT NULL DEFAULT 0,
        level           INTEGER NOT NULL DEFAULT 0,
        last_message_ts INTEGER NOT NULL DEFAULT 0,
        voice_seconds   INTEGER NOT NULL DEFAULT 0,
        updated_at      INTEGER NOT NULL DEFAULT 0
    );
    CREATE INDEX IF NOT EXISTS idx_community_xp_xp ON community_xp(xp DESC);
    """,
]


class Database:
    """aiosqlite 단일 커넥션 래퍼 + 증분 마이그레이션 러너."""

    def __init__(self, path: str) -> None:
        self._path = path
        self._conn: aiosqlite.Connection | None = None
        self._write_lock = asyncio.Lock()

    # ─── 수명주기 ─────────────────────────────────────────────────

    async def connect(self) -> None:
        self._conn = await aiosqlite.connect(self._path)
        self._conn.row_factory = aiosqlite.Row
        await self._conn.execute("PRAGMA journal_mode=WAL;")
        await self._conn.execute("PRAGMA foreign_keys=ON;")
        await self._conn.commit()
        await self._migrate()
        log.info("DB 연결: %s", self._path)

    async def close(self) -> None:
        if self._conn is not None:
            await self._conn.close()
            self._conn = None

    @property
    def conn(self) -> aiosqlite.Connection:
        if self._conn is None:
            raise RuntimeError("Database.connect() 가 선행되어야 합니다")
        return self._conn

    # ─── 마이그레이션 ─────────────────────────────────────────────

    async def _current_version(self) -> int:
        await self.conn.execute(
            "CREATE TABLE IF NOT EXISTS schema_meta (key TEXT PRIMARY KEY, value TEXT);"
        )
        await self.conn.commit()
        async with self.conn.execute(
            "SELECT value FROM schema_meta WHERE key = 'version'"
        ) as cur:
            row = await cur.fetchone()
        return int(row["value"]) if row else 0

    async def _migrate(self) -> None:
        current = await self._current_version()
        target = len(_MIGRATIONS)
        if current >= target:
            return
        for v in range(current, target):
            await self.conn.executescript(_MIGRATIONS[v])
            await self.conn.execute(
                "INSERT INTO schema_meta(key, value) VALUES('version', ?) "
                "ON CONFLICT(key) DO UPDATE SET value = excluded.value",
                (str(v + 1),),
            )
            await self.conn.commit()
            log.info("DB 마이그레이션 적용: v%d", v + 1)

    # ─── 쿼리 헬퍼 ────────────────────────────────────────────────
    # 쓰기는 lock 으로 직렬화. 읽기는 lock 불필요(단일 커넥션 순차).

    async def execute(self, sql: str, params: tuple = ()) -> int:
        """쓰기 쿼리 실행 + commit. lastrowid 반환."""
        async with self._write_lock:
            cur = await self.conn.execute(sql, params)
            await self.conn.commit()
            return cur.lastrowid

    async def fetchone(self, sql: str, params: tuple = ()) -> aiosqlite.Row | None:
        async with self.conn.execute(sql, params) as cur:
            return await cur.fetchone()

    async def fetchall(self, sql: str, params: tuple = ()) -> list[aiosqlite.Row]:
        async with self.conn.execute(sql, params) as cur:
            return list(await cur.fetchall())
