package com.poro.rpg.life.engine;

import java.util.Locale;

public enum LifeSourceType {
    FIELD("field"),
    ESTATE("estate");

    private final String code;

    LifeSourceType(String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }

    public static LifeSourceType from(String value) {
        String normalized = normalize(value);
        for (LifeSourceType type : values()) {
            if (type.code.equals(normalized)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown source_type: " + value);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
