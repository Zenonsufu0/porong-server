package kr.zenon.rpg.operations.query.model;

import java.time.Instant;
import java.util.Locale;
import java.util.Objects;

public record EconomyFlowRecord(
        Instant occurredAt,
        String direction,
        String source,
        long amount
) {
    public EconomyFlowRecord {
        Objects.requireNonNull(occurredAt, "occurredAt");
        direction = normalize(direction);
        source = normalize(source);
        amount = Math.max(0L, amount);
    }

    public boolean isInflow() {
        return "inflow".equals(direction);
    }

    public boolean isOutflow() {
        return "outflow".equals(direction);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
