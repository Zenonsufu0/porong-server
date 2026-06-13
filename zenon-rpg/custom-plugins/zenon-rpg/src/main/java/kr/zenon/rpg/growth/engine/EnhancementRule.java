package kr.zenon.rpg.growth.engine;

public record EnhancementRule(
        GrowthTier tier,
        int enhanceLevel,
        double successRate,
        long goldCost,
        long stoneCost,
        boolean breakOnFail,
        boolean downgradeOnFail
) {
}
