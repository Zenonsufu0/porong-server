package kr.zenon.rpg.life.engine;

import kr.zenon.rpg.common.registry.master.ItemMasterRegistry;
import kr.zenon.rpg.common.registry.master.model.ItemMaster;
import kr.zenon.rpg.common.seed.CsvRow;
import kr.zenon.rpg.common.seed.CsvSeedLoader;
import kr.zenon.rpg.common.time.TimeProvider;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LifeEstateEngineSampleTest {
    @Test
    void shouldRunLifeEstateFlowSample() {
        ItemMasterRegistry itemMasterRegistry = loadItemMasterRegistry();
        LifeGatherNodeRegistry gatherRegistry = loadGatherRegistry();
        LifeRecipeRegistry recipeRegistry = loadRecipeRegistry();
        LifeSkillExpRegistry expRegistry = loadExpRegistry();
        EstateUnlockRuleRegistry unlockRuleRegistry = loadUnlockRuleRegistry();
        EstateFacilityRegistry facilityRegistry = loadFacilityRegistry();
        EstateFacilityLevelRegistry facilityLevelRegistry = loadFacilityLevelRegistry();

        FixedTimeProvider timeProvider = new FixedTimeProvider(Instant.parse("2026-01-01T00:00:00Z"));
        QueueRandomProvider randomProvider = new QueueRandomProvider(
                List.of(0.50d, 0.01d, 0.50d, 0.60d, 0.50d, 0.60d, 0.50d, 0.60d, 0.50d, 0.60d),
                List.of(2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)
        );

        LifeSkillProgressionService progressionService = new LifeSkillProgressionService(expRegistry, new NoopLifeLevelUnlockHook());
        LifeGatherService gatherService = new LifeGatherService(gatherRegistry, progressionService, randomProvider);
        InMemoryLifeCraftLogHook craftLogHook = new InMemoryLifeCraftLogHook();
        LifeCraftService craftService = new LifeCraftService(recipeRegistry, craftLogHook, timeProvider);
        EstateUnlockService unlockService = new EstateUnlockService(
                unlockRuleRegistry,
                (state, questId) -> state.isQuestCompleted(questId),
                timeProvider
        );
        EstateFacilityService facilityService = new EstateFacilityService(
                facilityRegistry,
                facilityLevelRegistry,
                progressionService,
                new NoopEstateProductionAmountHook(),
                timeProvider
        );
        InMemoryEstateHarvestLogHook harvestLogHook = new InMemoryEstateHarvestLogHook();
        EstateHarvestService harvestService = new EstateHarvestService(
                facilityService,
                harvestLogHook,
                timeProvider,
                randomProvider
        );
        EstateSnapshotBuilder snapshotBuilder = new EstateSnapshotBuilder(harvestService, timeProvider);

        PlayerLifeState state = new PlayerLifeState("user_life_01");
        state.addCurrency("gold", 100_000L);
        state.markQuestCompleted("q_estate_unlock_01");

        LifeGatherService.GatherResult gatherExample = gatherService.gatherField(state, "field_herb_capital").orElseThrow();
        progressionService.gainExp(state, gatherExample.lifeType(), gatherExample.gainedExp()).orElseThrow();
        for (int i = 0; i < 11; i++) {
            LifeGatherService.GatherResult extra = gatherService.gatherField(state, "field_herb_capital").orElseThrow();
            progressionService.gainExp(state, extra.lifeType(), extra.gainedExp()).orElseThrow();
        }
        // 제국 약초 3개(refine_herb 요구량) + 밀 64개(potion_heal_minor 요구량)
        state.addItem("res_herb_imperial", 4);
        state.addItem("WHEAT", 128);
        state.addItem("POTATO", 64);
        state.addItem("CARROT", 64);

        LifeCraftService.CraftResult craftRefine = craftService.craft(state, "refine_herb", 1).orElseThrow();
        progressionService.gainExp(state, LifeType.REFINING, craftRefine.gainedExp()).orElseThrow();
        LifeCraftService.CraftResult craftPotion = craftService.craft(state, "potion_heal_minor", 1).orElseThrow();
        progressionService.gainExp(state, LifeType.ALCHEMY, craftPotion.gainedExp()).orElseThrow();

        EstateUnlockService.UnlockResult unlockResult = unlockService.unlock(state, "main_estate").orElseThrow();
        EstateFacilityService.InstallResult installHerb = facilityService.install(state, "estate_herb_plot", 1).orElseThrow();

        EstateFacilityService.UpgradeResult upgradeResult = facilityService.upgrade(state, 1).orElseThrow();

        timeProvider.advanceSeconds(4 * 3600L);
        EstateHarvestService.HarvestResult harvestResult = harvestService.harvestAll(state).orElseThrow();
        EstateSnapshotBuilder.EstateSnapshot snapshot = snapshotBuilder.build(state);

        assertTrue(gatherExample.sourceType() == LifeSourceType.FIELD);
        assertTrue(gatherExample.finalBaseAmount() >= 2);
        assertEquals("refine_herb", craftRefine.recipeId());
        assertEquals("potion_heal_minor", craftPotion.recipeId());
        assertEquals("main_estate", unlockResult.estateId());
        assertEquals("estate_herb_plot", installHerb.facilityId());
        assertEquals(2, upgradeResult.afterLevel());
        assertTrue(harvestResult.harvestedItems().size() >= 1);
        assertEquals(1, snapshot.facilities().size());
        assertTrue(!craftLogHook.entries().isEmpty());
        assertTrue(!harvestLogHook.entries().isEmpty());

        assertTrue(itemMasterRegistry.find("res_herb_imperial").isPresent());
        assertTrue(itemMasterRegistry.find("con_basic_healing_potion").isPresent());

        System.out.println("gather_example=source:" + gatherExample.sourceType().code()
                + " gather_id:" + gatherExample.gatherId()
                + " base:" + gatherExample.finalBaseAmount()
                + " rare_drop:" + gatherExample.rareDropped()
                + " rare_chance:" + gatherExample.finalRareChancePct()
                + " gained_exp:" + gatherExample.gainedExp());

        System.out.println("craft_example_1=recipe:" + craftRefine.recipeId()
                + " consumed:" + craftRefine.consumedMaterials()
                + " result:" + craftRefine.resultItemId() + " x" + craftRefine.resultAmount());
        System.out.println("craft_example_2=recipe:" + craftPotion.recipeId()
                + " consumed:" + craftPotion.consumedMaterials()
                + " result:" + craftPotion.resultItemId() + " x" + craftPotion.resultAmount());

        System.out.println("estate_upgrade_example=facility:" + upgradeResult.facilityId()
                + " before:" + upgradeResult.beforeLevel()
                + " after:" + upgradeResult.afterLevel()
                + " gold_cost:" + upgradeResult.goldCost()
                + " item_costs:" + upgradeResult.itemCosts());

        System.out.println("estate_harvest_example=items:" + harvestResult.harvestedItems()
                + " facility_results:" + harvestResult.facilityResults().stream()
                .map(result -> result.facilityId() + "@L" + result.level() + " cycles=" + result.cyclesProcessed())
                .collect(Collectors.joining(",")));

        System.out.println("flow_example=gather->craft->estate_install->harvest snapshot_facilities="
                + snapshot.facilities().stream()
                .map(facility -> facility.slotNo() + ":" + facility.facilityId() + "@L" + facility.level())
                .collect(Collectors.joining(",")));

        System.out.println("field_vs_estate_source=acquisition_totals:" + state.acquisitionSourceSnapshot());
    }

    private ItemMasterRegistry loadItemMasterRegistry() {
        Path path = Path.of("src/main/resources/seeds/item_master.csv");
        CsvSeedLoader<ItemMaster> loader = new CsvSeedLoader<>("item_master_life_test", path, this::mapItemMaster);
        ItemMasterRegistry registry = new ItemMasterRegistry();
        loader.load().orElseThrow().forEach(registry::register);
        return registry;
    }

    private LifeGatherNodeRegistry loadGatherRegistry() {
        Path path = Path.of("src/main/resources/seeds/life_gather_node.csv");
        CsvSeedLoader<LifeGatherNode> loader = new CsvSeedLoader<>("life_gather_node_test", path, LifeSeedMappers::gatherNode);
        LifeGatherNodeRegistry registry = new LifeGatherNodeRegistry();
        loader.load().orElseThrow().forEach(registry::register);
        return registry;
    }

    private LifeRecipeRegistry loadRecipeRegistry() {
        Path path = Path.of("src/main/resources/seeds/life_recipe_master.csv");
        CsvSeedLoader<LifeRecipe> loader = new CsvSeedLoader<>("life_recipe_master_test", path, LifeSeedMappers::recipe);
        LifeRecipeRegistry registry = new LifeRecipeRegistry();
        loader.load().orElseThrow().forEach(registry::register);
        return registry;
    }

    private LifeSkillExpRegistry loadExpRegistry() {
        Path path = Path.of("src/main/resources/seeds/life_skill_exp_table.csv");
        CsvSeedLoader<LifeSkillExpRule> loader = new CsvSeedLoader<>("life_skill_exp_table_test", path, LifeSeedMappers::skillExpRule);
        LifeSkillExpRegistry registry = new LifeSkillExpRegistry();
        loader.load().orElseThrow().forEach(registry::register);
        return registry;
    }

    private EstateUnlockRuleRegistry loadUnlockRuleRegistry() {
        Path path = Path.of("src/main/resources/seeds/estate_unlock_rule.csv");
        CsvSeedLoader<EstateUnlockRule> loader = new CsvSeedLoader<>("estate_unlock_rule_test", path, LifeSeedMappers::estateUnlockRule);
        EstateUnlockRuleRegistry registry = new EstateUnlockRuleRegistry();
        loader.load().orElseThrow().forEach(registry::register);
        return registry;
    }

    private EstateFacilityRegistry loadFacilityRegistry() {
        Path path = Path.of("src/main/resources/seeds/estate_facility_master.csv");
        CsvSeedLoader<EstateFacilityMaster> loader = new CsvSeedLoader<>("estate_facility_master_test", path, LifeSeedMappers::estateFacilityMaster);
        EstateFacilityRegistry registry = new EstateFacilityRegistry();
        loader.load().orElseThrow().forEach(registry::register);
        return registry;
    }

    private EstateFacilityLevelRegistry loadFacilityLevelRegistry() {
        Path path = Path.of("src/main/resources/seeds/estate_facility_level_rule.csv");
        CsvSeedLoader<EstateFacilityLevelRule> loader = new CsvSeedLoader<>("estate_facility_level_rule_test", path, LifeSeedMappers::estateFacilityLevelRule);
        EstateFacilityLevelRegistry registry = new EstateFacilityLevelRegistry();
        loader.load().orElseThrow().forEach(registry::register);
        return registry;
    }

    private ItemMaster mapItemMaster(CsvRow row) {
        return new ItemMaster(
                row.required("item_id"),
                row.required("item_name"),
                row.required("tier"),
                row.required("slot_type"),
                row.optional("region_theme"),
                row.optional("boss_source"),
                row.optional("set_id"),
                row.optional("base_stat_type"),
                row.optionalDouble("base_stat_value", 0.0d),
                row.optionalBoolean("is_tradeable", false),
                parseTags(row.optional("tags"))
        );
    }

    private Set<String> parseTags(String value) {
        if (value == null || value.isBlank()) {
            return Set.of();
        }
        return java.util.Arrays.stream(value.split("[|;]"))
                .map(tag -> tag.trim().toLowerCase(Locale.ROOT))
                .filter(tag -> !tag.isBlank() && !"-".equals(tag))
                .collect(Collectors.toUnmodifiableSet());
    }

    private static final class FixedTimeProvider implements TimeProvider {
        private Instant cursor;

        private FixedTimeProvider(Instant cursor) {
            this.cursor = cursor;
        }

        @Override
        public Instant nowInstant() {
            return cursor;
        }

        private void advanceSeconds(long seconds) {
            cursor = cursor.plusSeconds(seconds);
        }
    }

    private static final class QueueRandomProvider implements RandomProvider {
        private final Deque<Double> doubles;
        private final Deque<Integer> ints;

        private QueueRandomProvider(List<Double> doubles, List<Integer> ints) {
            this.doubles = new ArrayDeque<>(doubles);
            this.ints = new ArrayDeque<>(ints);
        }

        @Override
        public double nextDouble() {
            return doubles.isEmpty() ? 0.5d : doubles.removeFirst();
        }

        @Override
        public int nextInt(int boundExclusive) {
            if (boundExclusive <= 0) {
                return 0;
            }
            int raw = ints.isEmpty() ? 0 : ints.removeFirst();
            return Math.max(0, raw % boundExclusive);
        }
    }
}
