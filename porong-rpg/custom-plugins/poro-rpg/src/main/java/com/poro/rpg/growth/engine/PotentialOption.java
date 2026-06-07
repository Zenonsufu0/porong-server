package com.poro.rpg.growth.engine;

import java.util.Locale;

public record PotentialOption(
        String poolId,
        String optionCode,
        PotentialGrade grade,
        String slotType,
        String classFilter,
        String weaponFilter,
        String tagFilter,
        double valueMin,
        double valueMax,
        int weight
) {
    public PotentialOption {
        poolId = normalize(poolId);
        optionCode = normalize(optionCode);
        slotType = normalize(slotType);
        classFilter = normalize(classFilter);
        weaponFilter = normalize(weaponFilter);
        tagFilter = normalize(tagFilter);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
