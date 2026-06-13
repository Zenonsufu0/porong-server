package kr.zenon.rpg.growth.engine;

import kr.zenon.rpg.common.registry.master.ItemMasterRegistry;
import kr.zenon.rpg.common.registry.master.model.ItemMaster;
import kr.zenon.rpg.common.result.ErrorCode;
import kr.zenon.rpg.common.result.Result;

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

    // 등급 승급 확률 — 정본 potential_options_v1 §5-2 정합 (DL-129 추가#4, 기존 RARE/EPIC가 2~4배 과했음 교정).
    private static final Map<PotentialGrade, Double> UPGRADE_CHANCE_BY_GRADE = Map.of(
            PotentialGrade.COMMON, 0.25d,   // → RARE      (기댓값 4)
            PotentialGrade.RARE, 0.05d,     // → EPIC      (기댓값 20)
            PotentialGrade.EPIC, 0.01d,     // → UNIQUE    (기댓값 100)
            PotentialGrade.UNIQUE, 0.001d   // → LEGENDARY (기댓값 1,000)
    );

    // 확정 승급 천장 — 사장님 지시 "기댓값 × 2" = 2/확률. 비운의 꼬리(~10%)만 보호, 평균은 거의 불변.
    // 천장 카운터(item.pityCount)가 (천장−1)에 도달하면 다음 큐브에서 확정 승급. 승급 시 0으로 리셋(영속).
    private static final Map<PotentialGrade, Integer> PITY_CEILING_BY_GRADE = Map.of(
            PotentialGrade.COMMON, 8,       // → RARE
            PotentialGrade.RARE, 40,        // → EPIC
            PotentialGrade.EPIC, 200,       // → UNIQUE
            PotentialGrade.UNIQUE, 2000     // → LEGENDARY
    );

    /** 해당 등급의 다음 등급 승급 확률(0~1). 최고 등급이면 0. GUI 표시용. */
    public static double upgradeChanceFor(PotentialGrade grade) {
        return grade == null ? 0.0d : UPGRADE_CHANCE_BY_GRADE.getOrDefault(grade, 0.0d);
    }

    /** 해당 등급의 확정 승급 천장 횟수. 최고 등급이면 0. GUI 표시용. */
    public static int pityCeilingFor(PotentialGrade grade) {
        return grade == null ? 0 : PITY_CEILING_BY_GRADE.getOrDefault(grade, 0);
    }

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
        PotentialGrade curGrade = before.grade();
        PotentialGrade nextGrade = curGrade.nextOrNull();

        double chance = nextGrade != null ? UPGRADE_CHANCE_BY_GRADE.getOrDefault(curGrade, 0.0d) : 0.0d;
        int ceiling = nextGrade != null ? PITY_CEILING_BY_GRADE.getOrDefault(curGrade, Integer.MAX_VALUE) : Integer.MAX_VALUE;
        double roll = fixedRoll == null ? randomProvider.nextDouble() : fixedRoll;
        boolean natural = nextGrade != null && roll <= chance;
        // 천장: 이번 사용으로 카운터가 천장에 도달하면 확정 승급 (정본 §5-4, 천장=기댓값×2).
        boolean forced = nextGrade != null && item.pityCount() + 1 >= ceiling;
        boolean upgradeSuccess = natural || forced;
        PotentialGrade resultGrade = upgradeSuccess ? nextGrade : curGrade;
        // 천장 카운터: 승급(자연/확정) 시 0으로 리셋, 아니면 +1. 영속(PlayerEquipmentItem 직렬화).
        item.setPityCount(upgradeSuccess ? 0 : item.pityCount() + 1);

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
        lines.add(buildLine(1, state, itemMaster, grade));

        PotentialGrade secondGrade = randomProvider.nextDouble() < 0.10d ? grade : grade.lowerOrSame();
        lines.add(buildLine(2, state, itemMaster, secondGrade));

        PotentialGrade thirdGrade = randomProvider.nextDouble() < 0.05d ? grade : grade.lowerOrSame();
        lines.add(buildLine(3, state, itemMaster, thirdGrade));

        return new PotentialProfile(grade, lines.stream().sorted(Comparator.comparingInt(PotentialLine::lineNo)).toList());
    }

    /** 단일 weightedSelect 호출로 option code와 value를 함께 결정해 pair 일관성을 보장. */
    private PotentialLine buildLine(int lineNo, PlayerGrowthState state, ItemMaster itemMaster, PotentialGrade grade) {
        List<PotentialOption> filtered = filteredOptions(state, itemMaster, grade);
        if (filtered.isEmpty()) {
            return new PotentialLine(lineNo, grade, "generic_" + grade.name().toLowerCase(Locale.ROOT), 1.0d);
        }
        PotentialOption option = weightedSelect(filtered);
        double value = round2(option.valueMin() + (option.valueMax() - option.valueMin()) * randomProvider.nextDouble());
        return new PotentialLine(lineNo, grade, option.optionCode(), value);
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

    /** 아이템의 현재 풀에서 등장 가능한 옵션 코드(중복 제거·정렬). GUI "현재 장비 잠재 풀 표시"용 (DL-129 추가#4). */
    public List<String> poolOptionCodes(PlayerGrowthState state, ItemMaster itemMaster, PotentialGrade grade) {
        return filteredOptions(state, itemMaster, grade).stream()
                .map(PotentialOption::optionCode)
                .distinct()
                .sorted()
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
