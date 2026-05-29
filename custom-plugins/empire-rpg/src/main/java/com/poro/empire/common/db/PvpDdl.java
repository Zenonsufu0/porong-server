package com.poro.empire.common.db;

public final class PvpDdl {
    private PvpDdl() {}

    public static final String CREATE_PVP_RATING = """
        CREATE TABLE IF NOT EXISTS pvp_rating (
            player_uuid TEXT    PRIMARY KEY,
            player_name TEXT    NOT NULL,
            score       INTEGER NOT NULL DEFAULT 100,
            wins        INTEGER NOT NULL DEFAULT 0,
            losses      INTEGER NOT NULL DEFAULT 0,
            updated_at  INTEGER NOT NULL
        )
        """;

    public static final String CREATE_INDEX_RATING_SCORE =
            "CREATE INDEX IF NOT EXISTS idx_pvp_rating_score ON pvp_rating (score DESC)";

    public static final String CREATE_PVP_MATCH_LOG = """
        CREATE TABLE IF NOT EXISTS pvp_match_log (
            id          INTEGER PRIMARY KEY AUTOINCREMENT,
            match_type  TEXT    NOT NULL,           -- FREE/RANKED/FRIENDLY
            winner_uuid TEXT,                       -- 무승부 시 NULL
            loser_uuid  TEXT,                       -- 무승부 시 NULL
            draw        INTEGER NOT NULL DEFAULT 0,
            duration_s  INTEGER,
            reason      TEXT,                       -- 사망/타임아웃/서버이탈/...
            played_at   INTEGER NOT NULL
        )
        """;

    public static final String CREATE_INDEX_MATCH_TYPE =
            "CREATE INDEX IF NOT EXISTS idx_pvp_match_type ON pvp_match_log (match_type, played_at)";
}
