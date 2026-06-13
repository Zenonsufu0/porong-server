package kr.zenon.rpg.growth.engine;

import java.util.Locale;

public enum GrowthTier {
    T1,
    T2;

    public static GrowthTier from(String raw) {
        if (raw == null || raw.isBlank()) {
            return T1;
        }
        return switch (raw.trim().toUpperCase(Locale.ROOT)) {
            case "T2" -> T2;
            default -> T1;
        };
    }
}
