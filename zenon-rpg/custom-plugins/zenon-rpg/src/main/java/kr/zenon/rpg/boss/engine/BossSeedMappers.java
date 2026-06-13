package kr.zenon.rpg.boss.engine;

import kr.zenon.rpg.common.seed.CsvRow;

import java.util.Locale;

public final class BossSeedMappers {
    private BossSeedMappers() {
    }

    public static BossEntryRule entryRule(CsvRow row) {
        return new BossEntryRule(
                row.required("boss_id"),
                row.optional("requires_unlock_quest"),
                row.optionalInt("requires_party_size_min", 1),
                row.optionalInt("requires_party_size_max", 3),
                row.optional("requires_key_item"),
                nullableInt(row.optional("daily_limit")),
                nullableInt(row.optional("weekly_limit")),
                row.optional("notes")
        );
    }

    public static BossPattern pattern(CsvRow row) {
        return new BossPattern(
                row.required("boss_id"),
                row.optionalInt("phase_no", 1),
                row.required("pattern_id"),
                row.optional("pattern_group"),
                row.optional("priority"),
                row.optionalDouble("unlock_hp_threshold", 100.0d),
                row.optionalInt("cooldown_seconds", 0),
                row.optionalInt("max_consecutive_use", 1),
                row.optionalBoolean("is_forced", false),
                row.optional("condition_type"),
                row.optional("condition_value"),
                row.optional("success_branch_pattern_id"),
                row.optional("failure_branch_pattern_id"),
                row.optional("notes")
        );
    }

    private static Integer nullableInt(String raw) {
        if (raw == null || raw.isBlank() || "null".equals(raw.trim().toLowerCase(Locale.ROOT))) {
            return null;
        }
        return Integer.parseInt(raw.trim());
    }
}
