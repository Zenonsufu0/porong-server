package com.poro.rpg.growth.engine;

import java.util.Locale;

public record EngravingMaster(
        String engravingId,
        String engravingName,
        String engravingType,
        String classFilter,
        int maxLevel,
        String effectType,
        String valueType,
        double valuePerLevel,
        String notes
) {
    public EngravingMaster {
        engravingId = normalize(engravingId);
        engravingName = engravingName == null ? "" : engravingName.trim();
        engravingType = normalize(engravingType);
        classFilter = normalize(classFilter);
        effectType = normalize(effectType);
        valueType = normalize(valueType).toUpperCase(Locale.ROOT);
        notes = notes == null ? "" : notes.trim();
    }

    public boolean classType() {
        return "class".equals(engravingType);
    }

    public boolean commonType() {
        return "common".equals(engravingType);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
