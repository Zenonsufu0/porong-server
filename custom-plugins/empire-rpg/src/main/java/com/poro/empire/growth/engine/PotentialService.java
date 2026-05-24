package com.poro.empire.growth.engine;

import com.poro.empire.common.registry.master.ItemMasterRegistry;
import com.poro.empire.common.registry.master.model.ItemMaster;
import com.poro.empire.common.result.ErrorCode;
import com.poro.empire.common.result.Result;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public final class PotentialService {
    public static final String MATERIAL_CUBE = "mat_cube";
    private static final String MATERIAL_GOLD = "gold";
    private static final long CUBE_USE_GOLD_COST = 500L;

    private static final Map<PotentialGrade, Double> UPGRADE_CHANCE_BY_GRADE = Map.of(
            PotentialGrade.COMMON, 0.30d,
            PotentialGrade.RARE, 0.15d,
            PotentialGrade.EPIC, 0.075d,
            PotentialGrade.UNIQUE, 0.03d
    );

    private final ItemMasterRegistry itemMasterRegistry;
    private final PotentialOptionRegistry potentialOptionRegistry;
    private final PotentialSelectionHook potentialSelectionHook;
    private final RandomProvider randomProvider;

    public PotentialService(
            ItemMasterRegistry itemMasterRegistry,
            PotentialOptionRegistry potentialOptionRegistry,
            PotentialSelectionHook potentialSelectionHook,
            RandomProvider randomProvider
    ) {
        this.itemMasterRegistry = Objects.requireNonNull(itemMasterRegistry, "itemMasterRegistry");
        this.potentialOptionRegistry = Objects.requireNonNull(potentialOptionRegistry, "potentialOptionRegistry");
        this.potentialSelectionHook = Objects.requireNonNull(potentialSelectionHook, "potentialSelectionHook");
        this.randomProvider = Objects.requireNonNull(randomProvider, "randomProvider");
    }

    public Result<PotentialOperationResult> generateInitial(
            PlayerGrowthState state,
            String itemInstanceId,
            PotentialGrade initialGrade
    ) {
        PlayerEquipmentItem item = state.inventoryItem(itemInstanceId).orElse(null);
        if (item == null) {
            return Result.failure(ErrorCode.INVALID_ARGUMENT, "Item instance not found: " + itemInstanceId);
        }
        ItemMaster master = itemMasterRegistry.find(item.itemId()).orElse(null);
        if (master == null) {
            return Result.failure(ErrorCode.INVALID_ARGUMENT, "Unknown item master for potential generation: " + item.itemId());
        }
        PotentialProfile after = generateProfile(state, master, initialGrade);
        item.setPotentialProfile(after);
        return Result.success(new PotentialOperationResult(
                "generate",
                true,
                null,
                after,
                after,
                initialGrade.name(),
                initialGrade.name(),
                0.0d,
                1.0d
        ));
    }

    public Result<PotentialOperationResult> useCube(PlayerGrowthState state, String itemInstanceId) {
        return useCube(state, itemInstanceId, null);
    }

    public Result<PotentialOperationResult> useCube(PlayerGrowthState state, String itemInstanceId, Double fixedRoll) {
        PlayerEquipmentItem item = state.inventoryItem(itemInstanceId).orElse(null);
        if (item == null) {
            return Result.failure(ErrorCode.INVALID_ARGUMENT, "Item instance not found: " + itemInstanceId);
        }
        if (item.potentialProfile() == null) {
            return Result.failure(ErrorCode.INVALID_ARGUMENT, "Potential profile not initialized for item: " + item.itemInstanceId());
        }
        ItemMaster master = itemMasterRegistry.find(item.itemId()).orElse(null);
        if (master == null) {
            return Result.failure(ErrorCode.INVALID_ARGUMENT, "Unknown item master: " + item.itemId());
        }
        if (state.currency(MATERIAL_CUBE) < 1) {
            return Result.failure(ErrorCode.INVALID_ARGUMENT,
                    "Not enough mat_cube. required=1, current=" + state.currency(MATERIAL_CUBE));
        }
        if (state.currency(MATERIAL_GOLD) < CUBE_USE_GOLD_COST) {
            return Result.failure(ErrorCode.INVALID_ARGUMENT,
                    "Not enough gold. required=" + CUBE_USE_GOLD_COST + ", current=" + state.currency(MATERIAL_GOLD));
        }

        state.consumeCurrency(MATERIAL_CUBE, 1);
        state.consumeCurrency(MATERIAL_GOLD, CUBE_USE_GOLD_COST);

        PotentialProfile before = item.potentialProfile();
        PotentialGrade nextGrade = before.grade().nextOrNull();

        double chance = nextGrade != null ? UPGRADE_CHANCE_BY_GRADE.getOrDefault(before.grade(), 0.0d) : 0.0d;
        double roll = fixedRoll == null ? randomProvider.nextDouble() : fixedRoll;
        boolean upgradeSuccess = nextGrade != null && roll <= chance;
        PotentialGrade resultGrade = upgradeSuccess ? nextGrade : before.grade();

        PotentialProfile candidate = generateProfile(state, master, resultGrade);
        PotentialProfile selected = select("cube_use", state, item, before, candidate);
        item.setPotentialProfile(selected);

        return Result.success(new PotentialOperationResult(
                "cube_use",
                upgradeSuccess,
                before,
                candidate,
                selected,
                before.grade().name(),
                selected.grade().name(),
                roll,
                chance
        ));
    }

    public GrowthStatBlock buildPotentialStatBlock(PlayerGrowthState state) {
        GrowthStatBlock block = new GrowthStatBlock();
        for (PlayerEquipmentItem item : state.inventorySnapshot().values()) {
            PotentialProfile profile = item.potentialProfile();
            if (profile == null) {
                continue;
            }
            for (PotentialLine line : profile.lines()) {
                block.add(line.optionCode(), line.value());
            }
        }
        return block;
    }

    private PotentialProfile select(
            String operationType,
            PlayerGrowthState state,
            PlayerEquipmentItem item,
            PotentialProfile before,
            PotentialProfile after
    ) {
        PotentialProfile selected = potentialSelectionHook.choose(operationType, state, item, before, after);
        return selected == null ? after : selected;
    }

    private PotentialProfile generateProfile(PlayerGrowthState state, ItemMaster itemMaster, PotentialGrade grade) {
        List<PotentialLine> lines = new ArrayList<>();
        lines.add(new PotentialLine(1, grade, selectOptionCode(state, itemMaster, grade), selectOptionValue(state, itemMaster, grade)));

        PotentialGrade secondGrade = randomProvider.nextDouble() < 0.15d ? grade : grade.lowerOrSame();
        lines.add(new PotentialLine(2, secondGrade, selectOptionCode(state, itemMaster, secondGrade), selectOptionValue(state, itemMaster, secondGrade)));

        PotentialGrade thirdGrade = randomProvider.nextDouble() < 0.02d ? grade : grade.lowerOrSame();
        lines.add(new PotentialLine(3, thirdGrade, selectOptionCode(state, itemMaster, thirdGrade), selectOptionValue(state, itemMaster, thirdGrade)));

        return new PotentialProfile(grade, lines.stream().sorted(Comparator.comparingInt(PotentialLine::lineNo)).toList());
    }

    private String selectOptionCode(PlayerGrowthState state, ItemMaster itemMaster, PotentialGrade grade) {
        List<PotentialOption> filtered = filteredOptions(state, itemMaster, grade);
        if (filtered.isEmpty()) {
            return "generic_" + grade.name().toLowerCase(Locale.ROOT);
        }
        return weightedSelect(filtered).optionCode();
    }

    private double selectOptionValue(PlayerGrowthState state, ItemMaster itemMaster, PotentialGrade grade) {
        List<PotentialOption> filtered = filteredOptions(state, itemMaster, grade);
        if (filtered.isEmpty()) {
            return 1.0d;
        }
        PotentialOption selected = weightedSelect(filtered);
        double random = randomProvider.nextDouble();
        return round2(selected.valueMin() + (selected.valueMax() - selected.valueMin()) * random);
    }

    private List<PotentialOption> filteredOptions(PlayerGrowthState state, ItemMaster itemMaster, PotentialGrade grade) {
        String poolId = resolvePoolId(itemMaster);
        List<PotentialOption> inPool = potentialOptionRegistry.findByPoolAndGrade(poolId, grade);
        List<PotentialOption> filtered = inPool.stream()
                .filter(option -> option.slotType().isBlank() || option.slotType().equals(normalized(itemMaster.slotType())))
                .filter(option -> option.classFilter().isBlank() || "all".equals(option.classFilter()) || option.classFilter().equals(normalized(state.classId())))
                .toList();
        if (!filtered.isEmpty()) {
            return filtered;
        }
        return potentialOptionRegistry.all().values().stream()
                .filter(option -> option.grade() == grade)
                .toList();
    }

    private PotentialOption weightedSelect(List<PotentialOption> options) {
        int totalWeight = options.stream().mapToInt(option -> Math.max(1, option.weight())).sum();
        int roll = randomProvider.nextInt(totalWeight);
        int cursor = 0;
        for (PotentialOption option : options) {
            cursor += Math.max(1, option.weight());
            if (roll < cursor) {
                return option;
            }
        }
        return options.get(0);
    }

    private String resolvePoolId(ItemMaster itemMaster) {
        return normalized(itemMaster.tier()) + "_" + normalized(itemMaster.slotType()) + "_pool";
    }

    private String normalized(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private double round2(double value) {
        return Math.round(value * 100.0d) / 100.0d;
    }

    public record PotentialOperationResult(
            String operationType,
            boolean success,
            PotentialProfile before,
            PotentialProfile candidateAfter,
            PotentialProfile selectedAfter,
            String beforeGrade,
            String selectedGrade,
            double roll,
            double chance
    ) {
    }
}
