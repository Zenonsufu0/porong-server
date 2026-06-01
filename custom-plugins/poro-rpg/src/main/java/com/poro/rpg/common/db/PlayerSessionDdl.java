package com.poro.rpg.common.db;

/**
 * 접속 세션 로그 — 리텐션·DAU·플레이타임 판단용 (INBOX-004 #1).
 * join 시 1행 INSERT(quit_at NULL), quit 시 가장 최근 열린 세션을 UPDATE.
 * 서버 크래시로 quit 이벤트가 누락되면 해당 세션은 quit_at=NULL로 남아 플레이타임 집계에서 제외된다(DAU에는 포함).
 */
public final class PlayerSessionDdl {
    private PlayerSessionDdl() {}

    public static final String CREATE_PLAYER_SESSION_LOG = """
        CREATE TABLE IF NOT EXISTS player_session_log (
            id           INTEGER PRIMARY KEY AUTOINCREMENT,
            player_uuid  TEXT    NOT NULL,
            player_name  TEXT    NOT NULL,
            joined_at    INTEGER NOT NULL,           -- epoch ms
            quit_at      INTEGER,                    -- epoch ms, NULL=진행 중/크래시
            duration_s   INTEGER,                    -- quit 시 (quit_at-joined_at)/1000
            session_date TEXT    NOT NULL            -- 'YYYY-MM-DD' (joined_at 기준 서버 TZ, DAU 집계용)
        )
        """;

    public static final String CREATE_INDEX_SESSION_UUID =
            "CREATE INDEX IF NOT EXISTS idx_session_uuid ON player_session_log (player_uuid, joined_at)";

    public static final String CREATE_INDEX_SESSION_DATE =
            "CREATE INDEX IF NOT EXISTS idx_session_date ON player_session_log (session_date)";

    /** 진행 중(quit_at IS NULL) 세션 조회용 부분 인덱스 — quit 시 최근 열린 세션 빠른 매칭. */
    public static final String CREATE_INDEX_SESSION_OPEN =
            "CREATE INDEX IF NOT EXISTS idx_session_open ON player_session_log (player_uuid) WHERE quit_at IS NULL";
}
