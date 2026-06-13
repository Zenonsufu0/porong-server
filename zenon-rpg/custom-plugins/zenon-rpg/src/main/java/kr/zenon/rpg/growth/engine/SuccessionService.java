package kr.zenon.rpg.growth.engine;

import kr.zenon.rpg.common.result.ErrorCode;
import kr.zenon.rpg.common.result.Result;
import kr.zenon.rpg.growth.island.IslandTerritoryState;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 장비 전승 — 흔적 인스턴스의 등급·세부스탯을 대상 장비로 이전 (DL-129 추가#38, P3 재작성).
 *
 * <p>인스턴스화 이전(카운터형)에는 흔적 등급으로 세부스탯을 <em>전승 시점에</em> 롤했으나,
 * 이제 흔적이 드랍 시점에 롤된 등급+세부스탯을 들고 다니므로({@link TraceInstance}),
 * 전승은 그 인스턴스의 값을 <em>그대로 이전</em>하고 인스턴스 1개를 소모한다.</p>
 */
public final class SuccessionService {

    /** 구 스택형 흔적 id → 등급 매핑. P6 마이그레이션(스택→인스턴스 변환)·레거시 표시용으로 유지. */
    public static final Map<String, ItemGrade> TRACE_GRADE_MAP = Map.of(
        "equip_trace_broken",    ItemGrade.COMMON,
        "equip_trace_faded",     ItemGrade.RARE,
        "equip_trace_glowing",   ItemGrade.EPIC,
        "equip_trace_radiant",   ItemGrade.UNIQUE,
        "equip_trace_brilliant", ItemGrade.LEGENDARY
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
            String traceInstanceId,
            String targetInstanceId,
            SuccessionType type,
            ItemGrade appliedGrade,
            List<PotentialLine> appliedSubstats
    ) {}

    public static ItemGrade traceGrade(String traceId) {
        return TRACE_GRADE_MAP.getOrDefault(normalize(traceId), ItemGrade.COMMON);
    }

    public static boolean isKnownTrace(String traceId) {
        return TRACE_GRADE_MAP.containsKey(normalize(traceId));
    }

    /**
     * 흔적 인스턴스를 대상 장비에 전승한다.
     *
     * @param traceInstanceId 보유 흔적 인스턴스 id (islandState.traceInstances)
     * @param targetInstanceId 대상 장비 인스턴스 id (growthState.inventory)
     */
    public Result<SuccessionResult> apply(
            PlayerGrowthState growthState,
            IslandTerritoryState islandState,
            String traceInstanceId,
            String targetInstanceId,
            SuccessionType type
    ) {
        TraceInstance trace = islandState.findTraceInstance(traceInstanceId).orElse(null);
        if (trace == null) {
            return Result.failure(ErrorCode.INVALID_ARGUMENT, "흔적을 찾을 수 없습니다.");
        }
        PlayerEquipmentItem target = growthState.inventoryItem(targetInstanceId).orElse(null);
        if (target == null) {
            return Result.failure(ErrorCode.INVALID_ARGUMENT, "대상 아이템을 찾을 수 없습니다.");
        }
        long goldCost = type.goldCost();
        if (goldCost > 0 && !growthState.consumeCurrency("gold", goldCost)) {
            return Result.failure(ErrorCode.INVALID_ARGUMENT, "골드가 부족합니다. (필요: " + goldCost + "G)");
        }

        ItemGrade appliedGrade = null;
        List<PotentialLine> appliedSubstats = null;

        if (type == SuccessionType.BASIC || type == SuccessionType.GRADE_ONLY) {
            appliedGrade = trace.grade();
            target.setGrade(trace.grade());
        }
        if (type == SuccessionType.BASIC || type == SuccessionType.SUBSTAT_ONLY) {
            appliedSubstats = trace.substats();
            target.setSubstatLines(appliedSubstats);
        }

        islandState.removeTraceInstance(traceInstanceId);

        return Result.success(new SuccessionResult(traceInstanceId, targetInstanceId, type, appliedGrade, appliedSubstats));
    }

    private static String normalize(String s) {
        return s == null ? "" : s.trim().toLowerCase(Locale.ROOT);
    }
}
