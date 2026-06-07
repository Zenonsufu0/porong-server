package com.poro.rpg.operations.query.model;

import java.time.Instant;
import java.util.Locale;
import java.util.Objects;

public record LifeResourceSupplyRecord(
        Instant occurredAt,
        String itemId,
        String sourceType,
        long amount
) {
    public LifeResourceSupplyRecord {
        Objects.requireNonNull(occurredAt, "occurredAt");
        itemId = normalize(itemId);
        sourceType = normalize(sourceType);
        amount = Math.max(0L, amount);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
