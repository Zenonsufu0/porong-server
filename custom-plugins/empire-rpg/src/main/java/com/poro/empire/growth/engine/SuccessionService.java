package com.poro.empire.growth.engine;

import com.poro.empire.common.result.ErrorCode;
import com.poro.empire.common.result.Result;
import com.poro.empire.growth.island.IslandTerritoryState;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

public final class SuccessionService {

    public static final Map<String, ItemGrade> TRACE_GRADE_MAP = Map.of(
        "equip_trace_broken",       ItemGrade.COMMON,
        "equip_trace_faded",        ItemGrade.RARE,
        "equip_trace_glowing",      ItemGrade.EPIC,
        "equip_trace_radiant",      ItemGrade.UNIQUE,
        "equip_trace_brilliant",    ItemGrade.LEGENDARY,
        "equip_trace_unidentified", ItemGrade.COMMON
    );

    private static final Map<ItemGrade, PotentialGrade> GRADE_TO_POT = Map.of(
        ItemGrade.COMMON,    PotentialGrade.COMMON,
        ItemGrade.RARE,      PotentialGrade.RARE,
        ItemGrade.EPIC,      PotentialGrade.EPIC,
        ItemGrade.UNIQUE,    PotentialGrade.UNIQUE,
        ItemGrade.LEGENDARY, PotentialGrade.LEGENDARY
    );

    private static final Map<ItemGrade, Integer> GRADE_SUBSTAT_COUNT = Map.of(
        ItemGrade.COMMON,    1,
        ItemGrade.RARE,      2,
        ItemGrade.EPIC,      3,
        ItemGrade.UNIQUE,    3,
        ItemGrade.LEGENDARY, 3
    );

    public enum SuccessionType {
        BASIC,
        GRADE_ONLY,
        SUBSTAT_ONLY;

        public long goldCost() {
            return this == BASIC ? 0L : 100_000L;
        }

        public String displayName() {
            return switch (this) {
                case BASIC        -> "기본 전승 §7(무료)";
                case GRADE_ONLY   -> "등급전승권 §7(100,000G)";
                case SUBSTAT_ONLY -> "세부스탯전승권 §7(100,000G)";
            };
        }

        public SuccessionType next() {
            SuccessionType[] vals = values();
            return vals[(ordinal() + 1) % vals.length];
        }
    }

    public record SuccessionResult(
            String traceId,
            String targetInstanceId,
            SuccessionType type,
            ItemGrade appliedGrade,
            List<PotentialLine> appliedSubstats
    ) {}

    private final PotentialOptionRegistry potentialOptionRegistry;
    private final RandomProvider randomProvider;

    public SuccessionService(PotentialOptionRegistry potentialOptionRegistry, RandomProvider randomProvider) {
        this.potentialOptionRegistry = potentialOptionRegistry;
        this.randomProvider = randomProvider;
    }

    public static ItemGrade traceGrade(String traceId) {
        return TRACE_GRADE_MAP.getOrDefault(normalize(traceId), ItemGrade.COMMON);
    }

    public static boolean isKnownTrace(String traceId) {
        return TRACE_GRADE_MAP.containsKey(normalize(traceId));
    }

    public Result<SuccessionResult> apply(
            PlayerGrowthState growthState,
            IslandTerritoryState islandState,
            String traceId,
            String targetInstanceId,
            SuccessionType type
    ) {
        String normTrace = normalize(traceId);
        if (!TRACE_GRADE_MAP.containsKey(normTrace)) {
            return Result.failure(ErrorCode.INVALID_ARGUMENT, "유효하지 않은 흔적입니다: " + traceId);
        }
        if (islandState.getCustomItem(normTrace) < 1) {
            return Result.failure(ErrorCode.INVALID_ARGUMENT, "흔적이 부족합니다.");
        }
        PlayerEquipmentItem target = growthState.inventoryItem(targetInstanceId).orElse(null);
        if (target == null) {
            return Result.failure(ErrorCode.INVALID_ARGUMENT, "대상 아이템을 찾을 수 없습니다.");
        }
        long goldCost = type.goldCost();
        if (goldCost > 0 && !growthState.consumeCurrency("gold", goldCost)) {
            return Result.failure(ErrorCode.INVALID_ARGUMENT, "골드가 부족합니다. (필요: " + goldCost + "G)");
        }

        ItemGrade traceGrade = TRACE_GRADE_MAP.get(normTrace);
        ItemGrade appliedGrade = null;
        List<PotentialLine> appliedSubstats = null;

        if (type == SuccessionType.BASIC || type == SuccessionType.GRADE_ONLY) {
            appliedGrade = traceGrade;
            target.setGrade(traceGrade);
        }
        if (type == SuccessionType.BASIC || type == SuccessionType.SUBSTAT_ONLY) {
            appliedSubstats = generateSubstats(traceGrade);
            target.setSubstatLines(appliedSubstats);
        }

        islandState.withdrawCustomItem(normTrace, 1);

        return Result.success(new SuccessionResult(traceId, targetInstanceId, type, appliedGrade, appliedSubstats));
    }

    private List<PotentialLine> generateSubstats(ItemGrade grade) {
        PotentialGrade potGrade = GRADE_TO_POT.getOrDefault(grade, PotentialGrade.COMMON);
        int count = GRADE_SUBSTAT_COUNT.getOrDefault(grade, 1);

        List<PotentialOption> pool = potentialOptionRegistry.all().values().stream()
                .filter(o -> o.grade() == potGrade)
                .collect(Collectors.toList());

        if (pool.isEmpty()) return List.of();

        List<PotentialOption> shuffled = new ArrayList<>(pool);
        Collections.shuffle(shuffled);

        List<PotentialLine> result = new ArrayList<>();
        for (int i = 0; i < Math.min(count, shuffled.size()); i++) {
            PotentialOption opt = shuffled.get(i);
            double value = opt.valueMin() + randomProvider.nextDouble() * (opt.valueMax() - opt.valueMin());
            result.add(new PotentialLine(i + 1, opt.grade(), opt.optionCode(), value));
        }
        return result;
    }

    private static String normalize(String s) {
        return s == null ? "" : s.trim().toLowerCase(Locale.ROOT);
    }
}
