package com.poro.empire.boss.engine;

import com.poro.empire.boss.room.BossRoomManager;
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
    private static final String TRACE_BRILLIANT = "equip_trace_brilliant";
    private static final String TRACE_RADIANT   = "equip_trace_radiant";
    private static final String TRACE_GLOWING   = "equip_trace_glowing";
    private static final String TRACE_FADED     = "equip_trace_faded";
    private static final String TRACE_BROKEN    = "equip_trace_broken";
    private static final double CONTRIBUTION_THRESHOLD = 3.0;

    private final GrowthStateStore          growthStateStore;
    private final IslandTerritoryStateStore islandTerritoryStateStore;
    private final PlayerDataManager         playerDataManager;
    private final BossRoomManager           bossRoomManager;
    private final Logger                    logger;

    public BossRewardService(GrowthStateStore growthStateStore,
                             IslandTerritoryStateStore islandTerritoryStateStore,
                             PlayerDataManager playerDataManager,
                             BossRoomManager bossRoomManager,
                             Logger logger) {
        this.growthStateStore          = growthStateStore;
        this.islandTerritoryStateStore = islandTerritoryStateStore;
        this.playerDataManager         = playerDataManager;
        this.bossRoomManager           = bossRoomManager;
        this.logger                    = logger;
    }

    public void grantFieldBossReward(UUID killerUuid, int fieldIndex) {
        FieldBossRewardTable table = switch (fieldIndex) {
            // brilliant%, radiant%, glowing%, faded% — broken은 나머지
            case 2 -> new FieldBossRewardTable(4, 6,  5, 8,  45, 0.0, 5.0,  25.0, 45.0);
            case 3 -> new FieldBossRewardTable(6, 9,  5, 8,  55, 0.0, 5.0,  25.0, 45.0);
            case 4 -> new FieldBossRewardTable(7, 10, 8, 12, 65, 2.0, 13.0, 30.0, 35.0);
            case 5 -> new FieldBossRewardTable(9, 13, 10, 15, 75, 5.0, 25.0, 35.0, 25.0);
            default -> new FieldBossRewardTable(2, 4,  3, 5,  35, 0.0, 5.0,  25.0, 45.0);
        };
        String classId = playerDataManager.getWeaponType(killerUuid).name().toLowerCase(Locale.ROOT);
        PlayerGrowthState growth = growthStateStore.getOrCreate(killerUuid, classId);
        growth.addCurrency(ENHANCEMENT_STONE, randomInclusive(table.stoneMin(), table.stoneMax()));
        growth.addCurrency(CUBE_FRAGMENT, randomInclusive(table.cubeMin(), table.cubeMax()));
        if (roll(table.traceChancePct())) {
            islandTerritoryStateStore.getOrCreate(killerUuid)
                    .addCustomItem(pickTrace(table.brilliantTracePct(), table.radiantTracePct(), table.glowingTracePct(), table.fadedTracePct()), 1);
        }
        logger.info("[BossReward] field_boss field=" + fieldIndex + " uuid=" + killerUuid);
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
            bossRoomManager.markCleared(uuid, summary.bossId());
            rewarded++;
        }

        bossRoomManager.releaseByRunId(summary.runId());
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
                    .addCustomItem(pickTrace(table.brilliantTracePct(), table.radiantTracePct(), table.glowingTracePct(), table.fadedTracePct()), 1);
        }
    }

    private String pickTrace(double brilliantPct, double radiantPct, double glowingPct, double fadedPct) {
        double roll = ThreadLocalRandom.current().nextDouble(100.0);
        if (roll < brilliantPct)                                    return TRACE_BRILLIANT;
        if (roll < brilliantPct + radiantPct)                       return TRACE_RADIANT;
        if (roll < brilliantPct + radiantPct + glowingPct)          return TRACE_GLOWING;
        if (roll < brilliantPct + radiantPct + glowingPct + fadedPct) return TRACE_FADED;
        return TRACE_BROKEN;
    }

    private RewardTable tableFor(String bossId) {
        // B-tier first-clear defaults; S/A/C tiers require damage tracking (TODO)
        // brilliant%, radiant%, glowing%, faded% — broken은 나머지
        return switch (bossId.toLowerCase(Locale.ROOT)) {
            case "fallen_knight"  -> new RewardTable(5,  10, 25,  80,  2.0,  23.0, 45.0, 25.0);
            case "corrupted_lord" -> new RewardTable(6,  11, 28,  90,  2.0,  23.0, 45.0, 25.0);
            case "stone_colossus" -> new RewardTable(8,  14, 35,  100, 2.0,  23.0, 45.0, 25.0);
            case "storm_sorcerer" -> new RewardTable(10, 17, 45,  100, 2.0,  23.0, 45.0, 25.0);
            case "abyss_guardian" -> new RewardTable(12, 20, 55,  100, 2.0,  23.0, 45.0, 25.0);
            case "void_herald"    -> new RewardTable(15, 25, 70,  100, 2.0,  23.0, 45.0, 25.0);
            case "rift_king"      -> new RewardTable(25, 35, 100, 100, 15.0, 40.0, 35.0, 10.0);
            case "corrupted_dyad" -> new RewardTable(25, 35, 100, 100, 15.0, 40.0, 35.0, 10.0);
            case "spirit_watcher" -> new RewardTable(25, 35, 100, 100, 15.0, 40.0, 35.0, 10.0);
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

    private record FieldBossRewardTable(
            int stoneMin, int stoneMax,
            int cubeMin, int cubeMax,
            double traceChancePct,
            double brilliantTracePct, double radiantTracePct,
            double glowingTracePct, double fadedTracePct
    ) {}

    private record RewardTable(
            int stoneMin, int stoneMax,
            int cubeFixed,
            double traceChancePct,
            double brilliantTracePct, double radiantTracePct,
            double glowingTracePct, double fadedTracePct
    ) {}
}
