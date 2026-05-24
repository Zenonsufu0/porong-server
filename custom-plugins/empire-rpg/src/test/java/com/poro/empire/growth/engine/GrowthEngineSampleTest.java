package com.poro.empire.growth.engine;

import com.poro.empire.common.registry.master.ItemMasterRegistry;
import com.poro.empire.common.registry.master.model.ItemMaster;
import com.poro.empire.common.seed.CsvRow;
import com.poro.empire.common.seed.CsvSeedLoader;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GrowthEngineSampleTest {
    @Test
    void shouldRunGrowthSystemSamples() {
        ItemMasterRegistry itemMasterRegistry = loadItemMasterRegistry();
        EnhancementRuleRegistry enhancementRuleRegistry = loadEnhancementRuleRegistry();
        PotentialOptionRegistry potentialOptionRegistry = loadPotentialOptionRegistry();
        SetBonusRegistry setBonusRegistry = loadSetBonusRegistry();
        RuneRegistry runeRegistry = loadRuneRegistry();
        EngravingRegistry engravingRegistry = loadEngravingRegistry();

        QueueRandomProvider randomProvider = new QueueRandomProvider(
                List.of(0.31d, 0.40d, 0.01d, 0.70d, 0.80d, 0.20d, 0.15d, 0.60d),
                List.of(0, 0, 0, 0, 0, 0)
        );

        EquipmentService equipmentService = new EquipmentService(itemMasterRegistry, new NoopStatRecalculationHook());
        InMemoryEnhancementLogHook enhancementLogHook = new InMemoryEnhancementLogHook();
        EnhancementService enhancementService = new EnhancementService(
                itemMasterRegistry,
                enhancementRuleRegistry,
                enhancementLogHook,
                randomProvider
        );
        PotentialSelectionHook memorialHook = (operationType, state, item, before, after) -> after;
        PotentialService potentialService = new PotentialService(
                itemMasterRegistry,
                potentialOptionRegistry,
                memorialHook,
                randomProvider
        );
        SetBonusService setBonusService = new SetBonusService(itemMasterRegistry, setBonusRegistry);
        RuneService runeService = new RuneService(runeRegistry, itemMasterRegistry);
        EngravingService engravingService = new EngravingService(engravingRegistry);
        PlayerGrowthSnapshotBuilder snapshotBuilder = new PlayerGrowthSnapshotBuilder(
                itemMasterRegistry,
                setBonusService,
                runeService,
                engravingService,
                potentialService
        );

        PlayerGrowthState state = new PlayerGrowthState("user_growth_01", "warrior");
        state.addCurrency("gold", 500_000L);
        state.addCurrency("enhancement_stone", 500L);
        state.addCurrency("mat_cube", 5L);

        PlayerEquipmentItem weapon = new PlayerEquipmentItem("inst_weapon_01", "t2_gs_ragnes");
        PlayerEquipmentItem armorHead = new PlayerEquipmentItem("inst_armor_head_01", "t2_armor_ragnes_head");
        PlayerEquipmentItem armorChest = new PlayerEquipmentItem("inst_armor_chest_01", "t2_armor_ragnes_chest");
        PlayerEquipmentItem ring = new PlayerEquipmentItem("inst_ring_01", "t2_ring_carmen");
        ring.setEnhanceLevel(19);

        state.addInventoryItem(weapon);
        state.addInventoryItem(armorHead);
        state.addInventoryItem(armorChest);
        state.addInventoryItem(ring);

        equipmentService.equip(state, EquipmentSlot.WEAPON, weapon.itemInstanceId()).orElseThrow();
        equipmentService.equip(state, EquipmentSlot.ARMOR_HEAD, armorHead.itemInstanceId()).orElseThrow();
        equipmentService.equip(state, EquipmentSlot.ARMOR_CHEST, armorChest.itemInstanceId()).orElseThrow();
        equipmentService.equip(state, EquipmentSlot.ACCESSORY_1, ring.itemInstanceId()).orElseThrow();

        EnhancementService.EnhancementResult enhanceSuccess = enhancementService
                .attempt(state, weapon.itemInstanceId(), 0.01d)
                .orElseThrow();
        EnhancementService.EnhancementResult enhanceFail = enhancementService
                .attempt(state, ring.itemInstanceId(), 0.99d)
                .orElseThrow();

        PotentialService.PotentialOperationResult initPotential = potentialService
                .generateInitial(state, weapon.itemInstanceId(), PotentialGrade.RARE)
                .orElseThrow();
        PotentialService.PotentialOperationResult cubeResult = potentialService
                .useCube(state, weapon.itemInstanceId(), 0.10d)
                .orElseThrow();

        SetBonusService.SetBonusState setState = setBonusService.calculate(state);
        runeService.equip(state, 1, "rune_core_major").orElseThrow();
        engravingService.equipClassEngraving(state, "warrior_gs_crack_01").orElseThrow();
        engravingService.equipCommonEngraving(state, 1, "common_combat_01", 3).orElseThrow();
        engravingService.equipCommonEngraving(state, 2, "common_resource_01", 2).orElseThrow();
        engravingService.equipCommonEngraving(state, 3, "common_precision_01", 1).orElseThrow();

        PlayerGrowthSnapshotBuilder.PlayerGrowthSnapshot snapshot = snapshotBuilder.build(state);

        assertTrue(enhanceSuccess.success());
        assertEquals(1, enhanceSuccess.finalLevel());
        assertTrue(!enhanceFail.success());
        assertEquals(19, enhanceFail.finalLevel());
        assertEquals("RARE", initPotential.selectedAfter().grade().name());
        assertTrue(cubeResult.success());
        assertEquals("EPIC", cubeResult.selectedAfter().grade().name());
        assertEquals(3, setState.pieceCountBySet().getOrDefault("imperial_glory", 0));
        assertEquals("warrior_gs_crack_01", state.classEngravingId());
        assertEquals(3, state.commonEngravings().size());

        System.out.println("enhance_example_1=success item=" + enhanceSuccess.itemId()
                + " before=" + enhanceSuccess.beforeLevel()
                + " target=" + enhanceSuccess.targetLevel()
                + " final=" + enhanceSuccess.finalLevel()
                + " cost_gold=" + enhanceSuccess.goldCost()
                + " cost_stone=" + enhanceSuccess.stoneCost());
        System.out.println("enhance_example_2=fail item=" + enhanceFail.itemId()
                + " before=" + enhanceFail.beforeLevel()
                + " target=" + enhanceFail.targetLevel()
                + " final=" + enhanceFail.finalLevel()
                + " no_break=true no_downgrade=true");

        System.out.println("potential_example_1=cube_use success=" + cubeResult.success()
                + " before_grade=" + cubeResult.beforeGrade()
                + " after_grade=" + cubeResult.selectedGrade()
                + " line1=" + cubeResult.selectedAfter().lines().get(0).optionCode()
                + " chance=" + cubeResult.chance()
                + " roll=" + cubeResult.roll());

        System.out.println("set_bonus_example=imperial_glory pieces="
                + setState.pieceCountBySet().getOrDefault("imperial_glory", 0)
                + " active="
                + setState.activeRulesBySet().getOrDefault("imperial_glory", List.of()).stream()
                .map(rule -> rule.pieceCount() + "p:" + rule.effectType())
                .collect(Collectors.joining(",")));

        System.out.println("engraving_rune_example=class="
                + state.classEngravingId()
                + " common_slots=" + state.commonEngravings().size()
                + " rune_slot1=" + state.equippedRunes().getOrDefault(1, ""));

        System.out.println("growth_snapshot_example=enhancement_sum=" + snapshot.enhancementSum()
                + " set_piece_counts=" + snapshot.setBonusSummary().setPieceCounts()
                + " final_stats=" + snapshot.finalStatValues()
                + " flags=" + snapshot.finalStatFlags());
    }

    private ItemMasterRegistry loadItemMasterRegistry() {
        Path path = Path.of("src/main/resources/seeds/item_master.csv");
        CsvSeedLoader<ItemMaster> loader = new CsvSeedLoader<>("item_master_growth_test", path, this::mapItemMaster);
        ItemMasterRegistry registry = new ItemMasterRegistry();
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

    private EnhancementRuleRegistry loadEnhancementRuleRegistry() {
        Path path = Path.of("src/main/resources/seeds/growth_enhancement_table.csv");
        CsvSeedLoader<EnhancementRule> loader = new CsvSeedLoader<>("growth_enhancement_table_test", path, GrowthSeedMappers::enhancementRule);
        EnhancementRuleRegistry registry = new EnhancementRuleRegistry();
        loader.load().orElseThrow().forEach(registry::register);
        return registry;
    }

    private PotentialOptionRegistry loadPotentialOptionRegistry() {
        Path path = Path.of("src/main/resources/seeds/growth_potential_option_pool.csv");
        CsvSeedLoader<PotentialOption> loader = new CsvSeedLoader<>("growth_potential_option_pool_test", path, GrowthSeedMappers::potentialOption);
        PotentialOptionRegistry registry = new PotentialOptionRegistry();
        loader.load().orElseThrow().forEach(registry::register);
        return registry;
    }

    private SetBonusRegistry loadSetBonusRegistry() {
        Path path = Path.of("src/main/resources/seeds/growth_set_bonus.csv");
        CsvSeedLoader<SetBonusRule> loader = new CsvSeedLoader<>("growth_set_bonus_test", path, GrowthSeedMappers::setBonusRule);
        SetBonusRegistry registry = new SetBonusRegistry();
        loader.load().orElseThrow().forEach(registry::register);
        return registry;
    }

    private RuneRegistry loadRuneRegistry() {
        Path path = Path.of("src/main/resources/seeds/growth_rune_master.csv");
        CsvSeedLoader<RuneMaster> loader = new CsvSeedLoader<>("growth_rune_master_test", path, GrowthSeedMappers::runeMaster);
        RuneRegistry registry = new RuneRegistry();
        loader.load().orElseThrow().forEach(registry::register);
        return registry;
    }

    private EngravingRegistry loadEngravingRegistry() {
        Path path = Path.of("src/main/resources/seeds/growth_engraving_master.csv");
        CsvSeedLoader<EngravingMaster> loader = new CsvSeedLoader<>("growth_engraving_master_test", path, GrowthSeedMappers::engravingMaster);
        EngravingRegistry registry = new EngravingRegistry();
        loader.load().orElseThrow().forEach(registry::register);
        return registry;
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
