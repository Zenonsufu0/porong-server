package com.poro.rpg.life.engine;

import java.util.Locale;

public record EstateFacilityLevelRule(
        String facilityId,
        int level,
        int baseMin,
        int baseMax,
        double rareChancePercent,
        int storageHoursCap,
        long upgradeGoldCost,
        String basicCostItemId,
        int basicCostAmount,
        String themeCostItemId,
        int themeCostAmount
) {
    public EstateFacilityLevelRule {
        facilityId = normalize(facilityId);
        level = Math.max(1, level);
        baseMin = Math.max(1, baseMin);
        baseMax = Math.max(baseMin, baseMax);
        rareChancePercent = Math.max(0.0d, rareChancePercent);
        storageHoursCap = Math.max(4, storageHoursCap);
        upgradeGoldCost = Math.max(0L, upgradeGoldCost);
        basicCostItemId = normalize(basicCostItemId);
        basicCostAmount = Math.max(0, basicCostAmount);
        themeCostItemId = normalize(themeCostItemId);
        themeCostAmount = Math.max(0, themeCostAmount);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
