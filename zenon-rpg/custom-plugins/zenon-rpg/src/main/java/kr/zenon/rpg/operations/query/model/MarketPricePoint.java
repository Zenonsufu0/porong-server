package kr.zenon.rpg.operations.query.model;

import java.time.Instant;
import java.util.Locale;
import java.util.Objects;

public record MarketPricePoint(
        Instant capturedAt,
        String itemId,
        double avgPrice,
        long tradeVolume,
        long supplyVolume
) {
    public MarketPricePoint {
        Objects.requireNonNull(capturedAt, "capturedAt");
        itemId = normalize(itemId);
        avgPrice = Math.max(0.0d, avgPrice);
        tradeVolume = Math.max(0L, tradeVolume);
        supplyVolume = Math.max(0L, supplyVolume);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
