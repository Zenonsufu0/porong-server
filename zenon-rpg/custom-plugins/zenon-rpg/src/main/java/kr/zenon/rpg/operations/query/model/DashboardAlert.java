package kr.zenon.rpg.operations.query.model;

import java.time.Instant;
import java.util.Locale;
import java.util.Objects;

public record DashboardAlert(
        Instant occurredAt,
        String level,
        String code,
        String message
) {
    public DashboardAlert {
        Objects.requireNonNull(occurredAt, "occurredAt");
        level = normalize(level);
        code = normalize(code);
        message = message == null ? "" : message.trim();
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
