package com.poro.empire.common.db;

/**
 * 플레이어 성장 시계열 스냅샷 — 성장 곡선 판단용 (INBOX-004 #7 / DL-083).
 * 플레이어 JSON은 현재값 덮어쓰기라 "며칠차에 평균 몇 강/레벨인지" 추세를 알 수 없었다.
 * (player_uuid, snapshot_date) 일별 upsert — 하루 1행(그날 마지막 스냅샷). 일별 평균으로 성장 곡선 산출.
 */
public final class GrowthSnapshotDdl {
    private GrowthSnapshotDdl() {}

    public static final String CREATE_GROWTH_SNAPSHOT = """
        CREATE TABLE IF NOT EXISTS growth_snapshot (
            player_uuid   TEXT    NOT NULL,
            snapshot_date TEXT    NOT NULL,   -- 'YYYY-MM-DD' (서버 TZ)
            player_level  INTEGER NOT NULL,
            avg_il        REAL    NOT NULL,
            total_enhance INTEGER NOT NULL,
            gold          INTEGER NOT NULL,
            captured_at   INTEGER NOT NULL,   -- epoch ms (그날 마지막 갱신 시각)
            PRIMARY KEY (player_uuid, snapshot_date)
        )
        """;

    public static final String CREATE_INDEX_SNAP_DATE =
            "CREATE INDEX IF NOT EXISTS idx_growth_snap_date ON growth_snapshot (snapshot_date)";
}
