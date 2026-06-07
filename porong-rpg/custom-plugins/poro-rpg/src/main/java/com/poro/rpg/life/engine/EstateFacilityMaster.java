package com.poro.rpg.life.engine;

import java.util.Locale;

public record EstateFacilityMaster(
        String facilityId,
        String facilityName,
        String themeType,
        LifeType lifeType,
        String baseItemId,
        String rareItemId,
        int intervalMinutes,
        int maxLevel,
        String requiredBlueprintId
) {
    public EstateFacilityMaster {
        facilityId = normalize(facilityId);
        facilityName = facilityName == null ? "" : facilityName.trim();
        themeType = normalize(themeType);
        baseItemId = normalize(baseItemId);
        rareItemId = normalize(rareItemId);
        intervalMinutes = Math.max(1, intervalMinutes);
        maxLevel = Math.max(1, maxLevel);
        requiredBlueprintId = normalize(requiredBlueprintId);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
