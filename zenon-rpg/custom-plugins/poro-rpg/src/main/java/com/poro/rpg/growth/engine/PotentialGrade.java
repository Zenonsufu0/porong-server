package com.poro.rpg.growth.engine;

import java.util.Locale;

public enum PotentialGrade {
    COMMON,
    RARE,
    EPIC,
    UNIQUE,
    LEGENDARY;

    public static PotentialGrade from(String raw) {
        if (raw == null || raw.isBlank()) {
            return COMMON;
        }
        return valueOf(raw.trim().toUpperCase(Locale.ROOT));
    }

    public PotentialGrade lowerOrSame() {
        return switch (this) {
            case COMMON -> COMMON;
            case RARE -> COMMON;
            case EPIC -> RARE;
            case UNIQUE -> EPIC;
            case LEGENDARY -> UNIQUE;
        };
    }

    public PotentialGrade nextOrNull() {
        return switch (this) {
            case COMMON -> RARE;
            case RARE -> EPIC;
            case EPIC -> UNIQUE;
            case UNIQUE -> LEGENDARY;
            case LEGENDARY -> null;
        };
    }
}
