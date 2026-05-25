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

        boolean trackingAvailable = summary.participantSummaryPlaceholder().stream()
                .anyMatch(p -> ((Number) p.getOrDefault("damage_share", 0.0)).doubleValue() > 0.0);

        int rewarded = 0;
        for (Map<String, Object> participant : summary.participantSummaryPlaceholder()) {
            String userId = (String) participant.get("user_id");
            if (userId == null || userId.isBlank()) continue;

            if (trackingAvailable) {
                double damageShare = ((Number) participant.getOrDefault("damage_share", 0.0)).doubleValue();
                if (damageShare < CONTRIBUTION_THRESHOLD) continue;
            }

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
        // B-tier first-clear defaults; S/A/C tiers require damage tracking (TODO)
        return switch (bossId.toLowerCase(Locale.ROOT)) {
            case "fallen_knight"  -> new RewardTable(5,  10, 25,  80,  25, 70);
            case "corrupted_lord" -> new RewardTable(6,  11, 28,  90,  25, 70);
            case "stone_colossus" -> new RewardTable(8,  14, 35,  100, 25, 70);
            case "storm_sorcerer" -> new RewardTable(10, 17, 45,  100, 25, 70);
            case "abyss_guardian" -> new RewardTable(12, 20, 55,  100, 25, 70);
            case "void_herald"    -> new RewardTable(15, 25, 70,  100, 25, 70);
            case "rift_king"      -> new RewardTable(25, 35, 100, 100, 55, 45);
            case "corrupted_dyad" -> new RewardTable(25, 35, 100, 100, 55, 45);
            case "spirit_watcher" -> new RewardTable(25, 35, 100, 100, 55, 45);
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
