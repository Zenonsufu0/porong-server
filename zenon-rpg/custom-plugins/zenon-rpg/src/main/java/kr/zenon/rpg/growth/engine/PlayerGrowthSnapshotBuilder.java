package kr.zenon.rpg.growth.engine;

import kr.zenon.rpg.common.registry.master.ItemMasterRegistry;
import kr.zenon.rpg.common.registry.master.model.ItemMaster;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class PlayerGrowthSnapshotBuilder {
    private final ItemMasterRegistry itemMasterRegistry;
    private final SetBonusService setBonusService;
    private final RuneService runeService;
    private final EngravingService engravingService;
    private final PotentialService potentialService;

    public PlayerGrowthSnapshotBuilder(
            ItemMasterRegistry itemMasterRegistry,
            SetBonusService setBonusService,
            RuneService runeService,
            EngravingService engravingService,
            PotentialService potentialService
    ) {
        this.itemMasterRegistry = Objects.requireNonNull(itemMasterRegistry, "itemMasterRegistry");
        this.setBonusService = Objects.requireNonNull(setBonusService, "setBonusService");
        this.runeService = Objects.requireNonNull(runeService, "runeService");
        this.engravingService = Objects.requireNonNull(engravingService, "engravingService");
        this.potentialService = Objects.requireNonNull(potentialService, "potentialService");
    }

    public PlayerGrowthSnapshot build(PlayerGrowthState state) {
        List<EquippedItemSummary> equippedItems = new ArrayList<>();
        int enhancementSum = 0;
        Map<String, List<PotentialLineSummary>> potentialSummaryByItem = new LinkedHashMap<>();
        GrowthStatBlock baseStats = new GrowthStatBlock();

        for (Map.Entry<EquipmentSlot, String> entry : state.equippedItems().entrySet()) {
            PlayerEquipmentItem item = state.inventoryItem(entry.getValue()).orElse(null);
            if (item == null) {
                continue;
            }
            ItemMaster master = itemMasterRegistry.find(item.itemId()).orElse(null);
            if (master == null) {
                continue;
            }

            enhancementSum += item.enhanceLevel();
            if (master.baseStatType() != null && !master.baseStatType().isBlank()) {
                baseStats.add(master.baseStatType(), master.baseStatValue());
            }

            String potentialGrade = "";
            List<PotentialLineSummary> lineSummaries = List.of();
            if (item.potentialProfile() != null) {
                potentialGrade = item.potentialProfile().grade().name();
                lineSummaries = item.potentialProfile().lines().stream()
                        .map(line -> new PotentialLineSummary(line.lineNo(), line.grade().name(), line.optionCode(), line.value()))
                        .toList();
                potentialSummaryByItem.put(item.itemInstanceId(), lineSummaries);
            }

            equippedItems.add(new EquippedItemSummary(
                    entry.getKey().name(),
                    item.itemInstanceId(),
                    item.itemId(),
                    master.itemName(),
                    master.tier(),
                    master.slotType(),
                    master.setId(),
                    item.enhanceLevel(),
                    potentialGrade
            ));
        }

        SetBonusService.SetBonusState setBonusState = setBonusService.calculate(state);
        GrowthStatBlock finalStats = new GrowthStatBlock();
        finalStats.merge(baseStats);
        finalStats.merge(potentialService.buildPotentialStatBlock(state));
        finalStats.merge(setBonusService.applyToFinalStats(setBonusState));
        finalStats.merge(runeService.buildStatBlock(state));
        finalStats.merge(engravingService.buildStatBlock(state));

        Map<String, Integer> setPieceCounts = setBonusState.pieceCountBySet();
        Map<String, List<String>> setActiveEffects = new LinkedHashMap<>();
        for (Map.Entry<String, List<SetBonusRule>> entry : setBonusState.activeRulesBySet().entrySet()) {
            List<String> effects = entry.getValue().stream()
                    .map(rule -> rule.pieceCount() + "p:" + rule.effectType() + "=" + rule.valueAmount())
                    .toList();
            setActiveEffects.put(entry.getKey(), effects);
        }

        return new PlayerGrowthSnapshot(
                state.userId(),
                state.classId(),
                equippedItems,
                enhancementSum,
                Map.copyOf(potentialSummaryByItem),
                new SetBonusSummary(setPieceCounts, Map.copyOf(setActiveEffects)),
                new RuneSummary(state.equippedRunes()),
                new EngravingSummary(
                        state.classEngravingId(),
                        state.commonEngravings().entrySet().stream()
                                .collect(LinkedHashMap::new, (map, item) -> map.put(item.getKey(), item.getValue().engravingId() + ":lv" + item.getValue().level()), Map::putAll)
                ),
                finalStats.values(),
                finalStats.flags()
        );
    }

    public record PlayerGrowthSnapshot(
            String userId,
            String classId,
            List<EquippedItemSummary> equipmentSummary,
            int enhancementSum,
            Map<String, List<PotentialLineSummary>> potentialSummaryByItem,
            SetBonusSummary setBonusSummary,
            RuneSummary runeSummary,
            EngravingSummary engravingSummary,
            Map<String, Double> finalStatValues,
            java.util.Set<String> finalStatFlags
    ) {
    }

    public record EquippedItemSummary(
            String equippedSlot,
            String itemInstanceId,
            String itemId,
            String itemName,
            String tier,
            String slotType,
            String setId,
            int enhanceLevel,
            String potentialGrade
    ) {
    }

    public record PotentialLineSummary(
            int lineNo,
            String grade,
            String optionCode,
            double value
    ) {
    }

    public record SetBonusSummary(
            Map<String, Integer> setPieceCounts,
            Map<String, List<String>> activeEffects
    ) {
    }

    public record RuneSummary(
            Map<Integer, String> equippedRunes
    ) {
    }

    public record EngravingSummary(
            String classEngraving,
            Map<Integer, String> commonEngravings
    ) {
    }
}
