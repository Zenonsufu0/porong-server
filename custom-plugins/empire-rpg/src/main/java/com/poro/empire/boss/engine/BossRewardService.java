package com.poro.empire.boss.engine;

import com.poro.empire.growth.GrowthStateStore;
import com.poro.empire.growth.engine.PlayerGrowthState;
import com.poro.empire.growth.island.IslandTerritoryStateStore;
import com.poro.empire.storage.PlayerDataManager;

import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Logger;

public final class BossRewardService implements BossRewardResolverHook {
    private static final String ENHANCEMENT_STONE = "mat_stone_enhance";
    private static final String CUBE_FRAGMENT = "mat_cube_fragment";
    private static final String TRACE_GLOWING = "equip_trace_glowing";
    private static final String TRACE_FADED = "equip_trace_faded";
    private static final String TRACE_BROKEN = "equip_trace_broken";
    private static final double CONTRIBUTION_THRESHOLD = 3.0;

    private final GrowthStateStore growthStateStore;
    private final IslandTerritoryStateStore islandTerritoryStateStore;
    private final PlayerDataManager playerDataManager;
    private final Logger logger;

    public BossRewardService(GrowthStateStore growthStateStore,
                             IslandTerritoryStateStore islandTerritoryStateStore,
                             PlayerDataManager playerDataManager,
                             Logger logger) {
        this.growthStateStore = growthStateStore;
        this.islandTerritoryStateStore = islandTerritoryStateStore;
        this.playerDataManager = playerDataManager;
        this.logger = logger;
    }

    @Override
    public void onRunEnded(BossResultSummary summary) {
        if (!summary.clearSuccess()) return;

        RewardTable table = tableFor(summary.bossId());
        if (table == null) {
            logger.warning("[BossReward] No reward table for boss_id=" + summary.bossId());
            return;
        }

        int rewarded = 0;
        for (Map<String, Object> participant : summary.participantSummaryPlaceholder()) {
            String userId = (String) participant.get("user_id");
            if (userId == null || userId.isBlank()) continue;

            double damageShare = ((Number) participant.getOrDefault("damage_share", 0.0)).doubleValue();
            // damage_share == 0.0 means tracking is not yet wired; grant to all in that case
            boolean qualified = damageShare == 0.0 || damageShare >= CONTRIBUTION_THRESHOLD;
            if (!qualified) continue;

            UUID uuid;
            try {
                uuid = UUID.fromString(userId);
            } catch (IllegalArgumentException e) {
                logger.warning("[BossReward] Invalid UUID in participant user_id=" + userId);
                continue;
            }

            grantRewards(uuid, table);
            rewarded++;
        }

        logger.info("[BossReward] run_id=" + summary.runId()
                + " boss_id=" + summary.bossId()
                + " rewarded=" + rewarded + "/" + summary.participantSummaryPlaceholder().size());
    }

    private void grantRewards(UUID uuid, RewardTable table) {
        String classId = playerDataManager.getWeaponType(uuid).name().toLowerCase(Locale.ROOT);
        PlayerGrowthState growth = growthStateStore.getOrCreate(uuid, classId);

        growth.addCurrency(ENHANCEMENT_STONE, randomInclusive(table.stoneMin(), table.stoneMax()));
        growth.addCurrency(CUBE_FRAGMENT, table.cubeFixed());

        if (roll(table.traceChancePct())) {
            islandTerritoryStateStore.getOrCreate(uuid)
                    .addCustomItem(pickTrace(table.glowingTracePct(), table.fadedTracePct()), 1);
        }
    }

    private String pickTrace(double glowingPct, double fadedPct) {
        double roll = ThreadLocalRandom.current().nextDouble(100.0);
        if (roll < glowingPct) return TRACE_GLOWING;
        if (roll < glowingPct + fadedPct) return TRACE_FADED;
        return TRACE_BROKEN;
    }

    private RewardTable tableFor(String bossId) {
        // B-tier defaults: damage tracking not yet implemented
        return switch (bossId.toLowerCase(Locale.ROOT)) {
            case "earth_tyrant"   -> new RewardTable(5, 10, 25, 80, 70, 25);
            case "steel_arbiter"  -> new RewardTable(6, 11, 28, 90, 72, 23);
            case "abyss_overlord" -> new RewardTable(8, 14, 35, 100, 75, 22);
            default -> null;
        };
    }

    private boolean roll(double chancePct) {
        return ThreadLocalRandom.current().nextDouble(100.0) < chancePct;
    }

    private long randomInclusive(int min, int max) {
        if (min >= max) return min;
        return ThreadLocalRandom.current().nextLong(min, max + 1L);
    }

    private record RewardTable(
            int stoneMin,
            int stoneMax,
            int cubeFixed,
            double traceChancePct,
            double glowingTracePct,
            double fadedTracePct
    ) {}
}
