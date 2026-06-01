package com.poro.rpg.boss.engine;

import com.poro.rpg.boss.db.BossSessionRepository;
import com.poro.rpg.boss.room.BossRoomManager;
import com.poro.rpg.growth.GrowthStateStore;
import com.poro.rpg.growth.engine.PlayerGrowthState;
import com.poro.rpg.growth.island.IslandTerritoryStateStore;
import com.poro.rpg.storage.PlayerDataManager;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Logger;

public final class BossRewardService implements BossRewardResolverHook {
    private static final String ENHANCEMENT_STONE    = "mat_stone_enhance";
    private static final String BATTLE_SHARD         = "mat_battle_shard";
    private static final String CUBE_FRAGMENT        = "mat_cube_fragment";
    private static final String CUBE                 = "mat_cube";
    private static final String TRACE_BRILLIANT      = "equip_trace_brilliant";
    private static final String TRACE_RADIANT        = "equip_trace_radiant";
    private static final String TRACE_GLOWING        = "equip_trace_glowing";
    private static final String TRACE_FADED          = "equip_trace_faded";
    private static final String TRACE_BROKEN         = "equip_trace_broken";
    private static final String ANCIENT_TRACE_FADED    = "ancient_trace_faded";
    private static final String ANCIENT_TRACE_GLOWING  = "ancient_trace_glowing";
    private static final String ANCIENT_TRACE_RADIANT  = "ancient_trace_radiant";
    private static final String ANCIENT_TRACE_BRILLIANT = "ancient_trace_brilliant";
    private static final String COSMETIC_FRAGMENT       = "cosmetic_fragment";
    private static final String RIFT_KING_HEART         = "rift_king_heart";
    private static final double CONTRIBUTION_THRESHOLD = 3.0;
    private static final int    WEEKLY_BONUS_THRESHOLD = 10; // 주간 1~10회 보너스 / 11회+ 기본

    private final GrowthStateStore           growthStateStore;
    private final IslandTerritoryStateStore  islandTerritoryStateStore;
    private final PlayerDataManager          playerDataManager;
    private final BossRoomManager            bossRoomManager;
    private final Logger                     logger;

    // setter injection (BossEngineRuntime 이후 초기화)
    private BossSessionRepository            bossSessionRepository;
    private long                             seasonStartEpoch;

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

    /** BossEngineRuntime 초기화 후 주차 카운트용 컨텍스트 주입. */
    public void setSeasonContext(BossSessionRepository repository, long seasonStartEpoch) {
        this.bossSessionRepository = repository;
        this.seasonStartEpoch      = seasonStartEpoch;
    }

    private int computeSeasonWeek(long startedAtSeconds) {
        if (seasonStartEpoch <= 0 || startedAtSeconds < seasonStartEpoch) return 1;
        return (int) ((startedAtSeconds - seasonStartEpoch) / (7L * 24 * 3600)) + 1;
    }

    public void grantFieldBossReward(UUID killerUuid, int fieldIndex) {
        // equip_trace 등급 분포: CANON §2 정예몹 기준 (F1~3: 커먼60/레어35/에픽5, F4: +유니크2.5/레전더리0.5, F5: 커먼40/레어35/에픽18/유니크5/레전더리2)
        // brilliant%, radiant%, glowing%, faded% — broken은 나머지
        FieldBossRewardTable table = switch (fieldIndex) {
            case 2 -> new FieldBossRewardTable(4, 6,  5, 8,   4, 6,  45, 0.0, 0.0,  5.0, 35.0);
            case 3 -> new FieldBossRewardTable(6, 9,  5, 8,   4, 7,  55, 0.0, 0.0,  5.0, 35.0);
            case 4 -> new FieldBossRewardTable(7, 10, 8, 12,  4, 7,  65, 0.5, 2.5, 10.0, 35.0);
            case 5 -> new FieldBossRewardTable(9, 13, 10, 15, 5, 9,  75, 2.0, 5.0, 18.0, 35.0);
            default -> new FieldBossRewardTable(2, 4,  3, 5,  3, 5,  35, 0.0, 0.0,  5.0, 35.0);
        };
        String classId = playerDataManager.getWeaponType(killerUuid).name().toLowerCase(Locale.ROOT);
        PlayerGrowthState growth = growthStateStore.getOrCreate(killerUuid, classId);

        // 강화석
        growth.addCurrency(ENHANCEMENT_STONE, randomInclusive(table.stoneMin(), table.stoneMax()));

        // 큐브 조각 → 큐브 자동 전환 (10조각 = 1큐브)
        growth.addCurrency(CUBE_FRAGMENT, randomInclusive(table.cubeMin(), table.cubeMax()));
        long frags = growth.currency(CUBE_FRAGMENT);
        if (frags >= 10) {
            long newCubes = frags / 10;
            growth.consumeCurrency(CUBE_FRAGMENT, newCubes * 10);
            growth.addCurrency(CUBE, newCubes);
            logger.info("[Cube] " + killerUuid + " field_boss frags→cubes=" + newCubes);
        }

        // 전장의 파편 (island storage)
        islandTerritoryStateStore.getOrCreate(killerUuid)
                .addCustomItem(BATTLE_SHARD, randomInclusive(table.shardMin(), table.shardMax()));

        // 장비의 흔적 (equip_trace, 등급 랜덤)
        if (roll(table.traceChancePct())) {
            islandTerritoryStateStore.getOrCreate(killerUuid)
                    .addCustomItem(pickTrace(table.brilliantTracePct(), table.radiantTracePct(),
                            table.glowingTracePct(), table.fadedTracePct()), 1);
        }

        // 고대흔적 독립 드랍 (CANON §7.2 — 각 등급 독립 확률)
        grantAncientTraces(killerUuid, fieldIndex);

        logger.info("[BossReward] field_boss field=" + fieldIndex + " uuid=" + killerUuid);
    }

    @Override
    public void onRunEnded(BossResultSummary summary) {
        // 슬롯 해제는 클리어 여부와 무관하게 항상 실행
        bossRoomManager.releaseByRunId(summary.runId());

        if (!summary.clearSuccess()) return;

        BossSeasonRewards rewards = SEASON_REWARDS.get(summary.bossId().toLowerCase(Locale.ROOT));
        if (rewards == null) {
            logger.warning("[BossReward] No reward table for boss_id=" + summary.bossId());
            return;
        }

        int seasonWeek = computeSeasonWeek(summary.startedAt());
        int rewarded = 0;
        for (Map<String, Object> participant : summary.participantSummaryPlaceholder()) {
            String userId = (String) participant.get("user_id");
            if (userId == null || userId.isBlank()) continue;
            UUID uuid;
            try { uuid = UUID.fromString(userId); }
            catch (IllegalArgumentException e) {
                logger.warning("[BossReward] Invalid UUID in participant user_id=" + userId);
                continue;
            }

            // 주차 클리어 카운트 — DB 카운트는 이번 클리어 직전까지(이번 건은 아직 미기록 가능)
            int clearsBeforeThis = bossSessionRepository != null
                    ? bossSessionRepository.countClearsThisWeekAcrossBosses(uuid.toString(), seasonWeek)
                    : 0;
            boolean weeklyBonus = clearsBeforeThis < WEEKLY_BONUS_THRESHOLD;
            BossWeeklyReward table = weeklyBonus ? rewards.firstTen() : rewards.beyondTen();

            grantSeasonReward(uuid, table, weeklyBonus, summary.bossId());
            bossRoomManager.markCleared(uuid, summary.bossId());
            rewarded++;
        }

        logger.info("[BossReward] run_id=" + summary.runId()
                + " boss_id=" + summary.bossId()
                + " week=" + seasonWeek
                + " rewarded=" + rewarded + "/" + summary.participantSummaryPlaceholder().size());
    }

    private void grantSeasonReward(UUID uuid, BossWeeklyReward table, boolean bonusActive, String bossId) {
        String classId = playerDataManager.getWeaponType(uuid).name().toLowerCase(Locale.ROOT);
        PlayerGrowthState growth = growthStateStore.getOrCreate(uuid, classId);
        var territory = islandTerritoryStateStore.getOrCreate(uuid);

        // 강화석
        growth.addCurrency(ENHANCEMENT_STONE, randomInclusive(table.stoneMin(), table.stoneMax()));

        // 큐브 조각 → 큐브 자동 전환 (10조각=1큐브)
        if (table.cubeFragments() > 0) {
            growth.addCurrency(CUBE_FRAGMENT, table.cubeFragments());
            long frags = growth.currency(CUBE_FRAGMENT);
            if (frags >= 10) {
                long newCubes = frags / 10;
                growth.consumeCurrency(CUBE_FRAGMENT, newCubes * 10);
                growth.addCurrency(CUBE, newCubes);
                logger.info("[Cube] " + uuid + " season_boss frags→cubes=" + newCubes);
            }
        }

        if (!bonusActive) return; // 11회+ 부터는 강화석/큐브만

        // 1~10회 보너스: 장비 흔적 확정 N개
        for (int i = 0; i < table.guaranteedTraces(); i++) {
            territory.addCustomItem(pickSeasonTrace(), 1);
        }
        // 1~10회 보너스: 고대흔적 확정 (보스별 종류)
        if (table.ancientTraceId() != null) {
            territory.addCustomItem(table.ancientTraceId(), 1);
        }
        // 1~10회 보너스: 치장 제작 파편
        if (table.cosmeticFragMin() > 0) {
            territory.addCustomItem(COSMETIC_FRAGMENT,
                    randomInclusive(table.cosmeticFragMin(), table.cosmeticFragMax()));
        }
        // 균열왕 심장 (rift_king 1-10회 확정, 11회+ 0% — beyondTen에는 없음)
        if (table.heartGuaranteed()) {
            territory.addCustomItem(RIFT_KING_HEART, 1);
        }
    }

    /** 시즌보스 흔적 등급 분포 (커먼 5%, 레어 25%, 에픽 45%, 유니크 23%, 레전더리 2%). */
    private String pickSeasonTrace() {
        double roll = ThreadLocalRandom.current().nextDouble(100.0);
        if (roll <  2.0) return TRACE_BRILLIANT;
        if (roll < 25.0) return TRACE_RADIANT;
        if (roll < 70.0) return TRACE_GLOWING;
        if (roll < 95.0) return TRACE_FADED;
        return TRACE_BROKEN;
    }

    // §7.2 필드보스 고대흔적 — 각 등급 독립 확률 (F1: 빛바랜5%, F2: 빛바랜8%+빛나는1%, ...)
    private void grantAncientTraces(UUID uuid, int fieldIndex) {
        switch (fieldIndex) {
            case 1 -> tryAncientTrace(uuid, ANCIENT_TRACE_FADED,     5.0);
            case 2 -> {
                tryAncientTrace(uuid, ANCIENT_TRACE_FADED,    8.0);
                tryAncientTrace(uuid, ANCIENT_TRACE_GLOWING,  1.0);
            }
            case 3 -> {
                tryAncientTrace(uuid, ANCIENT_TRACE_FADED,   12.0);
                tryAncientTrace(uuid, ANCIENT_TRACE_GLOWING,  3.0);
            }
            case 4 -> {
                tryAncientTrace(uuid, ANCIENT_TRACE_FADED,    8.0);
                tryAncientTrace(uuid, ANCIENT_TRACE_GLOWING, 15.0);
                tryAncientTrace(uuid, ANCIENT_TRACE_RADIANT,  2.0);
            }
            case 5 -> {
                tryAncientTrace(uuid, ANCIENT_TRACE_GLOWING,  20.0);
                tryAncientTrace(uuid, ANCIENT_TRACE_RADIANT,   5.0);
                tryAncientTrace(uuid, ANCIENT_TRACE_BRILLIANT, 0.5);
            }
        }
    }

    private void tryAncientTrace(UUID uuid, String traceId, double chancePct) {
        if (roll(chancePct)) {
            islandTerritoryStateStore.getOrCreate(uuid).addCustomItem(traceId, 1);
            logger.info("[FieldBoss] ancient_trace=" + traceId + " uuid=" + uuid);
        }
    }

    private String pickTrace(double brilliantPct, double radiantPct, double glowingPct, double fadedPct) {
        double roll = ThreadLocalRandom.current().nextDouble(100.0);
        if (roll < brilliantPct)                                       return TRACE_BRILLIANT;
        if (roll < brilliantPct + radiantPct)                          return TRACE_RADIANT;
        if (roll < brilliantPct + radiantPct + glowingPct)             return TRACE_GLOWING;
        if (roll < brilliantPct + radiantPct + glowingPct + fadedPct)  return TRACE_FADED;
        return TRACE_BROKEN;
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
            int cubeMin,  int cubeMax,
            int shardMin, int shardMax,
            double traceChancePct,
            double brilliantTracePct, double radiantTracePct,
            double glowingTracePct,   double fadedTracePct
    ) {}

    // ─── 시즌 보스 보상 테이블 (DL-076) ─────────────────────────────────
    // 주간 1~10회: firstTen (강화석/큐브 많이 + 흔적 N개 확정 + 고대흔적 확정 + 치장 파편)
    // 주간 11회+: beyondTen (강화석/큐브 적게만)
    // S/A/B/C 등급 폐기. 1인/2인 인원 배율 1.0 (개인 지급).

    private record BossWeeklyReward(
            int stoneMin, int stoneMax,
            int cubeFragments,
            int guaranteedTraces,        // 1~10회만 적용 (장비 흔적 확정 개수)
            String ancientTraceId,       // 1~10회만 적용 (null = 없음)
            int cosmeticFragMin, int cosmeticFragMax,
            boolean heartGuaranteed      // 균열왕 심장 (rift_king 1-10회만)
    ) {
        public static BossWeeklyReward weeklyOnly(int stoneMin, int stoneMax, int cube) {
            return new BossWeeklyReward(stoneMin, stoneMax, cube, 0, null, 0, 0, false);
        }
    }

    private record BossSeasonRewards(BossWeeklyReward firstTen, BossWeeklyReward beyondTen) {}

    private static final Map<String, BossSeasonRewards> SEASON_REWARDS = new HashMap<>();
    static {
        // drop_tables_v1.md §4 — S 클리어 보상 (1~10회) / S 재도전 보상 (11회+)
        SEASON_REWARDS.put("fallen_knight",  new BossSeasonRewards(
                new BossWeeklyReward(15, 20,  70, 1, ANCIENT_TRACE_FADED,     2, 3, false),
                BossWeeklyReward.weeklyOnly(5,  8,  20)));
        SEASON_REWARDS.put("corrupted_lord", new BossSeasonRewards(
                new BossWeeklyReward(18, 25,  85, 1, ANCIENT_TRACE_FADED,     2, 3, false),
                BossWeeklyReward.weeklyOnly(6,  9,  28)));
        SEASON_REWARDS.put("stone_colossus", new BossSeasonRewards(
                new BossWeeklyReward(22, 30, 110, 2, ANCIENT_TRACE_GLOWING,   3, 5, false),
                BossWeeklyReward.weeklyOnly(7, 10,  20)));
        SEASON_REWARDS.put("storm_sorcerer", new BossSeasonRewards(
                new BossWeeklyReward(26, 36, 140, 2, ANCIENT_TRACE_GLOWING,   4, 6, false),
                BossWeeklyReward.weeklyOnly(9, 13,  30)));
        SEASON_REWARDS.put("abyss_guardian", new BossSeasonRewards(
                new BossWeeklyReward(30, 42, 165, 2, ANCIENT_TRACE_RADIANT,   5, 7, false),
                BossWeeklyReward.weeklyOnly(11, 15, 25)));
        SEASON_REWARDS.put("void_herald",    new BossSeasonRewards(
                new BossWeeklyReward(35, 48, 190, 2, ANCIENT_TRACE_BRILLIANT, 5, 8, false),
                BossWeeklyReward.weeklyOnly(13, 18, 28)));
        // 최종보스 3종 동일 보상 구조 (rift_king만 심장 추가)
        SEASON_REWARDS.put("rift_king",      new BossSeasonRewards(
                new BossWeeklyReward(40, 50, 210, 2, ANCIENT_TRACE_BRILLIANT, 6, 8, true),
                BossWeeklyReward.weeklyOnly(15, 20, 30)));
        SEASON_REWARDS.put("corrupted_dyad", new BossSeasonRewards(
                new BossWeeklyReward(40, 50, 210, 2, ANCIENT_TRACE_BRILLIANT, 6, 8, false),
                BossWeeklyReward.weeklyOnly(15, 20, 30)));
        SEASON_REWARDS.put("spirit_watcher", new BossSeasonRewards(
                new BossWeeklyReward(40, 50, 210, 2, ANCIENT_TRACE_BRILLIANT, 6, 8, false),
                BossWeeklyReward.weeklyOnly(15, 20, 30)));
    }
}
