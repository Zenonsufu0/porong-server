package kr.zenon.rpg.growth.engine;

import java.util.Locale;

public record RuneMaster(
        String runeId,
        String runeName,
        String runeGrade,
        String effectType,
        String valueType,
        double valueAmount,
        String slotFilter,
        String notes
) {
    public RuneMaster {
        runeId = normalize(runeId);
        runeName = runeName == null ? "" : runeName.trim();
        runeGrade = normalize(runeGrade);
        effectType = normalize(effectType);
        valueType = normalize(valueType).toUpperCase(Locale.ROOT);
        slotFilter = normalize(slotFilter);
        notes = notes == null ? "" : notes.trim();
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
