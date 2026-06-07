package com.poro.rpg.life.engine;

import java.util.Locale;

public record LifeGatherNode(
        String gatherId,
        String gatherName,
        LifeType lifeType,
        LifeSourceType sourceType,
        String baseItemId,
        int baseMin,
        int baseMax,
        String rareItemId,
        double rareChanceMin,
        double rareChanceMax,
        int expGain,
        int rareExpBonusMin,
        int rareExpBonusMax,
        String themeType
) {
    public LifeGatherNode {
        gatherId = normalize(gatherId);
        gatherName = gatherName == null ? "" : gatherName.trim();
        baseItemId = normalize(baseItemId);
        rareItemId = normalize(rareItemId);
        themeType = normalize(themeType);
        baseMin = Math.max(1, baseMin);
        baseMax = Math.max(baseMin, baseMax);
        rareChanceMin = Math.max(0.0d, rareChanceMin);
        rareChanceMax = Math.max(rareChanceMin, rareChanceMax);
        expGain = Math.max(0, expGain);
        rareExpBonusMin = Math.max(0, rareExpBonusMin);
        rareExpBonusMax = Math.max(rareExpBonusMin, rareExpBonusMax);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
