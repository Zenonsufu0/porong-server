package com.poro.rpg.growth.engine;

import java.util.Locale;

public record SetBonusRule(
        String setId,
        String setName,
        int pieceCount,
        int effectOrder,
        String effectType,
        String valueType,
        double valueAmount,
        String notes
) {
    public SetBonusRule {
        setId = normalize(setId);
        setName = setName == null ? "" : setName.trim();
        effectType = normalize(effectType);
        valueType = normalize(valueType).toUpperCase(Locale.ROOT);
        notes = notes == null ? "" : notes.trim();
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
