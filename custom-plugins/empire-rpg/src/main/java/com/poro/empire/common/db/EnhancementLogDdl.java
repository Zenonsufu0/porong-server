package com.poro.empire.common.db;

/**
 * 강화 시도 로그 — 강화 성공률 검증·소모량 판단용 (INBOX-004 #3).
 * 기존 in-memory 로그(InMemoryEnhancementLogHook)는 재시작 시 소실되어 누적 판단이 불가했다.
 * 이 테이블로 DB 영속화하여 표기 성공률 vs 실제, 유저당 강화 부담(골드·강화석)을 판단 가능.
 */
public final class EnhancementLogDdl {
    private EnhancementLogDdl() {}

    public static final String CREATE_ENHANCEMENT_LOG = """
        CREATE TABLE IF NOT EXISTS enhancement_log (
            id             INTEGER PRIMARY KEY AUTOINCREMENT,
            player_uuid    TEXT    NOT NULL,
            item_id        TEXT    NOT NULL,
            tier           TEXT    NOT NULL,
            before_level   INTEGER NOT NULL,
            target_level   INTEGER NOT NULL,
            final_level    INTEGER NOT NULL,
            success        INTEGER NOT NULL,   -- 1/0
            success_rate   REAL    NOT NULL,   -- 표기 성공률(%)
            roll           REAL    NOT NULL,   -- 굴림값 (천장 강제 성공 시 -1)
            gold_cost      INTEGER NOT NULL,
            stone_cost     INTEGER NOT NULL,
            forced_ceiling INTEGER NOT NULL,   -- 천장 강제 성공 1/0
            attempted_at   INTEGER NOT NULL    -- epoch ms
        )
        """;

    public static final String CREATE_INDEX_ENH_TIER_TARGET =
            "CREATE INDEX IF NOT EXISTS idx_enh_tier_target ON enhancement_log (tier, target_level)";

    public static final String CREATE_INDEX_ENH_PLAYER =
            "CREATE INDEX IF NOT EXISTS idx_enh_player ON enhancement_log (player_uuid)";
}
