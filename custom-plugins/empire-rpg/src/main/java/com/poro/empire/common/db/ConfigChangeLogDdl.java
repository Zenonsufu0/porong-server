package com.poro.empire.common.db;

/**
 * 런타임 설정 변경 감사 로그 (INBOX-010 축 C).
 *
 * <p>모든 런타임 설정 변경(몹 스탯·상점 등)을 1행씩 기록. 변경 추적 + 향후 디스코드/웹
 * 패치노트 자동 피드의 원천. "런타임 편의 ↔ 패치노트 추적" 트레이드오프를 자동화로 해소.</p>
 */
public final class ConfigChangeLogDdl {
    private ConfigChangeLogDdl() {}

    public static final String CREATE_TABLE = """
        CREATE TABLE IF NOT EXISTS config_change_log (
            id            INTEGER PRIMARY KEY AUTOINCREMENT,
            changed_at    INTEGER NOT NULL,
            changed_by    TEXT,
            domain        TEXT    NOT NULL,
            target_key    TEXT    NOT NULL,
            field         TEXT    NOT NULL,
            old_value     TEXT,
            new_value     TEXT
        )
        """;

    public static final String CREATE_INDEX_CHANGED_AT =
        "CREATE INDEX IF NOT EXISTS idx_config_change_log_changed_at ON config_change_log(changed_at)";
}
