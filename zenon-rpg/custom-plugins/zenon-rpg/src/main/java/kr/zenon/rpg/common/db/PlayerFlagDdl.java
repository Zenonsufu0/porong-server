package kr.zenon.rpg.common.db;

public final class PlayerFlagDdl {
    private PlayerFlagDdl() {}

    public static final String CREATE_TABLE = """
        CREATE TABLE IF NOT EXISTS player_flag (
            player_uuid   TEXT    NOT NULL,
            flag_key      TEXT    NOT NULL,
            value_type    TEXT    NOT NULL CHECK (value_type IN ('BOOL','LONG','STRING')),
            value_text    TEXT,
            value_long    INTEGER,
            updated_at    INTEGER NOT NULL,
            version       INTEGER NOT NULL DEFAULT 1,
            PRIMARY KEY (player_uuid, flag_key)
        )
        """;

    public static final String CREATE_INDEX_UPDATED_AT =
        "CREATE INDEX IF NOT EXISTS idx_player_flag_updated_at ON player_flag(updated_at)";

    public static final String CREATE_INDEX_FLAG_KEY =
        "CREATE INDEX IF NOT EXISTS idx_player_flag_flag_key ON player_flag(flag_key)";
}
