package com.poro.empire.common.db;

public final class BossSessionDdl {
    private BossSessionDdl() {}

    public static final String CREATE_SESSION_LOG = """
        CREATE TABLE IF NOT EXISTS boss_session_log (
            id                      INTEGER PRIMARY KEY AUTOINCREMENT,
            boss_id                 TEXT    NOT NULL,
            season_week             INTEGER NOT NULL,
            started_at              INTEGER NOT NULL,
            ended_at                INTEGER,
            result                  TEXT    NOT NULL DEFAULT 'abandoned', -- 진행 중 초기값. 종료 시 UPDATE. 뷰는 ended_at IS NOT NULL로 필터. (DL-065)
            clear_time_seconds      INTEGER,
            party_size              INTEGER NOT NULL,
            party_avg_enhance       REAL,
            party_avg_il            REAL,
            max_defense_ignore_pct  REAL    DEFAULT 0.0,
            has_defense_ignore      INTEGER DEFAULT 0
        )
        """;

    public static final String CREATE_INDEX_BOSS_WEEK =
        "CREATE INDEX IF NOT EXISTS idx_bsl_boss_week ON boss_session_log (boss_id, season_week)";

    public static final String CREATE_INDEX_RESULT =
        "CREATE INDEX IF NOT EXISTS idx_bsl_result ON boss_session_log (boss_id, result)";

    public static final String CREATE_SESSION_PLAYER = """
        CREATE TABLE IF NOT EXISTS boss_session_player (
            id                  INTEGER PRIMARY KEY AUTOINCREMENT,
            session_id          INTEGER NOT NULL REFERENCES boss_session_log(id),
            player_uuid         TEXT    NOT NULL,
            weapon_enhance      INTEGER DEFAULT 0,
            avg_enhance         REAL    DEFAULT 0.0,
            il                  REAL    DEFAULT 0.0,
            defense_ignore_pct  REAL    DEFAULT 0.0,
            damage_share        REAL    DEFAULT 0.0     -- 데미지 기여 점유율(%) (DL-084)
        )
        """;

    public static final String CREATE_INDEX_SESSION =
        "CREATE INDEX IF NOT EXISTS idx_bsp_session ON boss_session_player (session_id)";

    /** 기존 boss_session_player에 damage_share 컬럼 추가 (DL-084). 컬럼 존재 시 건너뜀(idempotent). */
    public static final String ALTER_SESSION_PLAYER_DAMAGE_SHARE =
        "ALTER TABLE boss_session_player ADD COLUMN damage_share REAL DEFAULT 0.0";

    public static final String DROP_STATS_SUMMARY_VIEW =
        "DROP VIEW IF EXISTS boss_stats_summary";

    /** ended_at IS NOT NULL 조건으로 진행 중 세션을 집계에서 제외한다. */
    public static final String CREATE_STATS_SUMMARY_VIEW = """
        CREATE VIEW IF NOT EXISTS boss_stats_summary AS
        SELECT
            boss_id,
            COUNT(*) AS total_attempts,
            SUM(CASE WHEN result = 'clear'   THEN 1 ELSE 0 END) AS total_clears,
            ROUND(100.0 * SUM(CASE WHEN result = 'clear' THEN 1 ELSE 0 END) / COUNT(*), 1)
                AS clear_rate_pct,
            ROUND(AVG(CASE WHEN result = 'clear' THEN clear_time_seconds END), 0)
                AS avg_clear_seconds,
            ROUND(100.0 * SUM(CASE WHEN result = 'timeout' THEN 1 ELSE 0 END) / COUNT(*), 1)
                AS timeout_rate_pct,
            ROUND(100.0 * SUM(has_defense_ignore) / COUNT(*), 1)
                AS defense_ignore_rate_pct,
            ROUND(AVG(party_avg_enhance), 1) AS avg_party_enhance,
            ROUND(AVG(party_avg_il), 1)      AS avg_party_il
        FROM boss_session_log
        WHERE ended_at IS NOT NULL
        GROUP BY boss_id
        """;
}
