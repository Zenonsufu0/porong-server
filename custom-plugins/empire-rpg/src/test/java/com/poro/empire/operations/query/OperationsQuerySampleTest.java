package com.poro.empire.operations.query;

import com.poro.empire.boss.engine.BossEngineRuntime;
import com.poro.empire.boss.engine.BossResultSummary;
import com.poro.empire.boss.engine.InMemoryBossRunRecordHook;
import com.poro.empire.common.registry.master.BossMasterRegistry;
import com.poro.empire.common.registry.master.model.BossMaster;
import com.poro.empire.growth.engine.GrowthEngineRuntime;
import com.poro.empire.growth.engine.InMemoryEnhancementLogHook;
import com.poro.empire.growth.engine.PlayerGrowthSnapshotBuilder;
import com.poro.empire.life.engine.EstateHarvestLogEntry;
import com.poro.empire.life.engine.EstateSnapshotBuilder;
import com.poro.empire.life.engine.InMemoryEstateHarvestLogHook;
import com.poro.empire.life.engine.InMemoryLifeCraftLogHook;
import com.poro.empire.life.engine.LifeCraftLogEntry;
import com.poro.empire.life.engine.LifeEngineRuntime;
import com.poro.empire.operations.query.api.AdminApiEndpointLayer;
import com.poro.empire.operations.query.discord.DiscordCardResponse;
import com.poro.empire.operations.query.discord.DiscordCommandQueryAdapter;
import com.poro.empire.operations.query.model.DashboardAlert;
import com.poro.empire.operations.query.model.EconomyFlowRecord;
import com.poro.empire.operations.query.model.MarketPricePoint;
import com.poro.empire.operations.query.model.PlayerProfileRecord;
import com.poro.empire.operations.query.model.QueryTimeRange;
import com.poro.empire.operations.query.service.AdminDashboardService;
import com.poro.empire.operations.query.service.BossStatisticsQueryService;
import com.poro.empire.operations.query.service.HallOfFameQueryService;
import com.poro.empire.operations.query.service.LifeStatisticsQueryService;
import com.poro.empire.operations.query.service.MarketStatisticsQueryService;
import com.poro.empire.operations.query.service.PlayerDetailQueryService;
import com.poro.empire.operations.query.service.PublicSnapshotQueryService;
import com.poro.empire.operations.query.store.InMemoryOperationsDataStore;
import com.poro.empire.quest.engine.InMemoryHallOfFameHook;
import com.poro.empire.quest.engine.InMemoryQuestRecordRewardHook;
import com.poro.empire.quest.engine.InMemorySymbolicHonorHook;
import com.poro.empire.quest.engine.PlayerQuestAndHonorSnapshotBuilder;
import com.poro.empire.quest.engine.PlayerQuestHonorState;
import com.poro.empire.quest.engine.QuestAchievementRuntime;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

class OperationsQuerySampleTest {
    @Test
    void shouldBuildAdminAndDiscordQueryResponses() {
        QueryTimeRange range = new QueryTimeRange(
                Instant.parse("2026-04-12T00:00:00Z"),
                Instant.parse("2026-04-13T00:00:00Z"),
                "today"
        );

        InMemoryOperationsDataStore dataStore = new InMemoryOperationsDataStore();

        InMemoryBossRunRecordHook bossHook = new InMemoryBossRunRecordHook();
        bossHook.onRunEnded(new BossResultSummary(
                "run_001",
                "ragnes",
                true,
                4,
                611L,
                2,
                "",
                List.of(
                        Map.of("user_id", "user_001", "deaths", 0),
                        Map.of("user_id", "user_002", "deaths", 1)
                ),
                Instant.parse("2026-04-12T10:00:00Z").getEpochSecond(),
                3
        ));
        bossHook.onRunEnded(new BossResultSummary(
                "run_002",
                "carmen",
                false,
                3,
                994L,
                0,
                "battle_fail",
                List.of(
                        Map.of("user_id", "user_001", "deaths", 2),
                        Map.of("user_id", "user_003", "deaths", 3)
                ),
                Instant.parse("2026-04-12T11:00:00Z").getEpochSecond(),
                2
        ));
        BossEngineRuntime bossRuntime = new BossEngineRuntime(null, null, null, bossHook, null);

        InMemoryLifeCraftLogHook craftLogHook = new InMemoryLifeCraftLogHook();
        craftLogHook.onCrafted(new LifeCraftLogEntry(
                Instant.parse("2026-04-12T10:00:00Z"),
                "user_001",
                "potion_heal_normal",
                1,
                Map.of("res_herb_imperial", 2L),
                "con_basic_healing_potion",
                1
        ));
        InMemoryEstateHarvestLogHook harvestLogHook = new InMemoryEstateHarvestLogHook();
        harvestLogHook.onHarvested(new EstateHarvestLogEntry(
                Instant.parse("2026-04-12T11:00:00Z"),
                "user_001",
                "main_estate",
                2,
                Map.of("res_herb_imperial", 6L, "res_ore_resonance", 4L)
        ));
        LifeEngineRuntime lifeRuntime = new LifeEngineRuntime(
                null, null, null, null, null, null, null,
                null, null, null, null, null, null,
                craftLogHook, harvestLogHook
        );

        InMemoryHallOfFameHook hallHook = new InMemoryHallOfFameHook();
        hallHook.onAchievementRecorded(
                new PlayerQuestHonorState("user_001"),
                new com.poro.empire.common.registry.master.model.AchievementMaster(
                        "ach_extreme_carmen",
                        "special",
                        "Extreme Vanguard",
                        "EXTREME_CLEAR",
                        "carmen",
                        1,
                        "RECORD",
                        "hall_of_fame_extreme",
                        1,
                        false,
                        false,
                        ""
                ),
                "hall_of_fame_extreme",
                1
        );
        QuestAchievementRuntime questRuntime = new QuestAchievementRuntime(
                null, null, null, null, null, null, null,
                null, null,
                new InMemoryQuestRecordRewardHook(),
                hallHook,
                new InMemorySymbolicHonorHook()
        );

        GrowthEngineRuntime growthRuntime = new GrowthEngineRuntime(
                null, null, null, null, null, null, null, null,
                null, null, null, null, null, new InMemoryEnhancementLogHook()
        );

        BossMasterRegistry bossMasterRegistry = new BossMasterRegistry();
        bossMasterRegistry.register(new BossMaster("ragnes", "Ragnes", "final", "south", true, false));
        bossMasterRegistry.register(new BossMaster("carmen", "Carmen", "extreme", "arkanon", true, true));

        dataStore.upsertPlayerProfile(new PlayerProfileRecord("user_001", "PoroHero", "warrior"));
        dataStore.upsertGrowthSnapshot("user_001", sampleGrowthSnapshot());
        dataStore.upsertLifeSnapshot("user_001", sampleLifeSnapshot());
        dataStore.upsertQuestSnapshot("user_001", sampleQuestSnapshot());
        dataStore.upsertLifeLevels("user_001", Map.of("herb_gathering", 12, "alchemy", 7));

        dataStore.addMarketPricePoint(new MarketPricePoint(Instant.parse("2026-04-12T08:00:00Z"), "res_herb_imperial", 120.0d, 300, 500));
        dataStore.addMarketPricePoint(new MarketPricePoint(Instant.parse("2026-04-12T20:00:00Z"), "res_herb_imperial", 144.0d, 420, 640));
        dataStore.addMarketPricePoint(new MarketPricePoint(Instant.parse("2026-04-12T20:00:00Z"), "con_basic_healing_potion", 510.0d, 170, 210));
        dataStore.addEconomyFlow(new EconomyFlowRecord(Instant.parse("2026-04-12T09:00:00Z"), "inflow", "boss_reward", 2_400_000));
        dataStore.addEconomyFlow(new EconomyFlowRecord(Instant.parse("2026-04-12T09:20:00Z"), "outflow", "enhancement", 1_100_000));
        dataStore.addAlert(new DashboardAlert(Instant.parse("2026-04-12T21:00:00Z"), "warning", "BOSS_CLEAR_ZERO", "carmen clear rate dropped"));
        dataStore.markTranscendenceHolder("user_001");

        BossStatisticsQueryService bossService = new BossStatisticsQueryService(bossMasterRegistry, bossRuntime, dataStore);
        MarketStatisticsQueryService marketService = new MarketStatisticsQueryService(dataStore);
        LifeStatisticsQueryService lifeService = new LifeStatisticsQueryService(lifeRuntime, dataStore);
        HallOfFameQueryService hallService = new HallOfFameQueryService(questRuntime, bossRuntime, dataStore);
        PlayerDetailQueryService playerService = new PlayerDetailQueryService(dataStore, bossRuntime);
        PublicSnapshotQueryService publicService = new PublicSnapshotQueryService(playerService, marketService, hallService);
        AdminDashboardService adminService = new AdminDashboardService(bossService, marketService, lifeService, dataStore);
        AdminApiEndpointLayer adminApi = new AdminApiEndpointLayer(adminService, playerService);
        DiscordCommandQueryAdapter discord = new DiscordCommandQueryAdapter(publicService);

        AdminDashboardService.OverviewSummary overview = adminService.overviewSummary(range);
        BossStatisticsQueryService.BossSummaryResponse bossSummary = adminService.bossStatisticsSummary(range);
        PlayerDetailQueryService.PlayerDetailProjection playerDetail = playerService.query("user_001");
        MarketStatisticsQueryService.MarketPriceSnapshot marketPrice = marketService.queryItemPrice("res_herb_imperial", range);
        DiscordCardResponse discordEquip = discord.handleCommand("/내장비", "user_001", "", range);
        DiscordCardResponse discordMarket = discord.handleCommand("/시세", "user_001", "res_herb_imperial", range);

        assertTrue(overview.alertCount() >= 1);
        assertTrue(!bossSummary.bosses().isEmpty());
        assertTrue(playerDetail.equipment().enhancementSum() >= 0);
        assertTrue(marketPrice.avgPrice() > 0.0d);
        assertTrue(!discordEquip.lines().isEmpty());
        assertTrue(!discordMarket.lines().isEmpty());

        System.out.println("admin_overview_example=" + overview);
        System.out.println("boss_summary_example=" + bossSummary.bosses().stream().limit(2).toList());
        System.out.println("player_detail_example=" + playerDetail);
        System.out.println("market_price_example=" + marketPrice);
        System.out.println("discord_naejangbi_example=" + discordEquip);
        System.out.println("discord_sise_example=" + discordMarket);
        System.out.println("admin_api_endpoints=" + adminApi.listEndpoints());
    }

    private PlayerGrowthSnapshotBuilder.PlayerGrowthSnapshot sampleGrowthSnapshot() {
        return new PlayerGrowthSnapshotBuilder.PlayerGrowthSnapshot(
                "user_001",
                "warrior",
                List.of(
                        new PlayerGrowthSnapshotBuilder.EquippedItemSummary("WEAPON", "inst_weapon_01", "t2_gs_ragnes", "Crimson Greatsword", "T2", "weapon", "crimson_brand", 18, "EPIC")
                ),
                18,
                Map.of("inst_weapon_01", List.of(
                        new PlayerGrowthSnapshotBuilder.PotentialLineSummary(1, "EPIC", "atk_percent", 12.0d),
                        new PlayerGrowthSnapshotBuilder.PotentialLineSummary(2, "RARE", "crit_rate", 4.0d),
                        new PlayerGrowthSnapshotBuilder.PotentialLineSummary(3, "RARE", "hp_percent", 3.0d)
                )),
                new PlayerGrowthSnapshotBuilder.SetBonusSummary(
                        Map.of("imperial_glory", 3),
                        Map.of("imperial_glory", List.of("3p:general_damage_increase=6.0"))
                ),
                new PlayerGrowthSnapshotBuilder.RuneSummary(Map.of(1, "rune_core_major")),
                new PlayerGrowthSnapshotBuilder.EngravingSummary("warrior_gs_crack_01", Map.of(1, "common_combat_01:lv3")),
                Map.of("attack", 455.0d, "defense", 210.0d),
                java.util.Set.of("playstyle_completion_after_dash_combo")
        );
    }

    private EstateSnapshotBuilder.EstateSnapshot sampleLifeSnapshot() {
        return new EstateSnapshotBuilder.EstateSnapshot(
                "main_estate",
                Instant.parse("2026-04-10T00:00:00Z"),
                true,
                List.of(
                        new EstateSnapshotBuilder.FacilitySnapshot(
                                1,
                                "estate_herb_plot",
                                3,
                                Instant.parse("2026-04-12T11:00:00Z"),
                                1,
                                Map.of("res_herb_imperial", 4L)
                        )
                ),
                new EstateSnapshotBuilder.RecentHarvestSnapshot(
                        Instant.parse("2026-04-12T11:00:00Z"),
                        2,
                        Map.of("res_herb_imperial", 6L, "res_ore_resonance", 4L)
                )
        );
    }

    private PlayerQuestAndHonorSnapshotBuilder.Snapshot sampleQuestSnapshot() {
        return new PlayerQuestAndHonorSnapshotBuilder.Snapshot(
                "user_001",
                Instant.parse("2026-04-12T23:00:00Z"),
                List.of(),
                8,
                List.of(
                        new PlayerQuestAndHonorSnapshotBuilder.FeaturedAchievementSummary(
                                "ach_clear_ragnes",
                                "Flamebreaker",
                                "boss",
                                "BOSS_CLEAR",
                                true
                        )
                ),
                "title_ragnes_clear",
                "badge_first_craft"
        );
    }
}
