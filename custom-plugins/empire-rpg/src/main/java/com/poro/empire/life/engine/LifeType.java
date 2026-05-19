package com.poro.empire.life.engine;

import java.util.Locale;

public enum LifeType {
    HERB_GATHERING("herb_gathering"),
    MINING("mining"),
    LOGGING("logging"),
    FISHING("fishing"),
    COOKING("cooking"),
    ALCHEMY("alchemy"),
    REFINING("refining"),
    CRAFTING("crafting"),
    ESTATE_MANAGEMENT("estate_management");

    private final String code;

    LifeType(String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }

    public static LifeType from(String value) {
        String normalized = normalize(value);
        for (LifeType type : values()) {
            if (type.code.equals(normalized)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown life_type: " + value);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
