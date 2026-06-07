package com.poro.rpg.operations.query.model;

import java.time.Instant;
import java.util.Locale;
import java.util.Objects;

public record ConsumableUsageRecord(
        Instant occurredAt,
        String itemId,
        long useCount
) {
    public ConsumableUsageRecord {
        Objects.requireNonNull(occurredAt, "occurredAt");
        itemId = normalize(itemId);
        useCount = Math.max(0L, useCount);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
