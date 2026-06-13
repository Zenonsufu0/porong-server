package kr.zenon.rpg.life.engine;

import java.util.Locale;

public record LifeSkillExpRule(
        LifeType lifeType,
        int level,
        long cumulativeExp,
        double gatherSpeedBonusPct,
        double yieldBonusPct,
        double rareBonusPct,
        double estateOutputBonusPct,
        String unlockCode
) {
    public LifeSkillExpRule {
        level = Math.max(1, level);
        cumulativeExp = Math.max(0L, cumulativeExp);
        gatherSpeedBonusPct = Math.max(0.0d, gatherSpeedBonusPct);
        yieldBonusPct = Math.max(0.0d, yieldBonusPct);
        rareBonusPct = Math.max(0.0d, rareBonusPct);
        estateOutputBonusPct = Math.max(0.0d, estateOutputBonusPct);
        unlockCode = normalize(unlockCode);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
