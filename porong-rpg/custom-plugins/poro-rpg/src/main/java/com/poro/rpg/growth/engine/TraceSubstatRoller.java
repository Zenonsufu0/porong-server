package com.poro.rpg.growth.engine;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 장비 흔적 세부스탯 롤러 (DL-129 추가#38, P1b).
 *
 * <p>흔적 전용 슬롯-무관 통합풀({@code growth_trace_substat_pool.csv})에서 등급별 개수만큼
 * 세부스탯을 롤한다. 개수 규칙·값 균등롤은 결정2(현행 규칙 계승)를 따른다 —
 * COMMON 1 / RARE 2 / EPIC·UNIQUE·LEGENDARY 3, 값은 {@code value_min~value_max} 균등.</p>
 *
 * <p>{@link SuccessionService#generateSubstats}와 동일한 롤 방식이지만, 슬롯 필터 대신
 * 흔적 전용 풀을 쓴다(결정1 범용 — 드랍 시점에 대상 슬롯을 모르므로 슬롯-무관 풀에서만 롤).</p>
 */
public final class TraceSubstatRoller {

    // 등급별 세부스탯 개수 — SuccessionService.GRADE_SUBSTAT_COUNT와 동일 규칙 (결정2).
    private static final Map<ItemGrade, Integer> COUNT = Map.of(
            ItemGrade.COMMON,    1,
            ItemGrade.RARE,      2,
            ItemGrade.EPIC,      3,
            ItemGrade.UNIQUE,    3,
            ItemGrade.LEGENDARY, 3
    );

    private final PotentialOptionRegistry pool; // 흔적 전용 슬롯-무관 풀
    private final RandomProvider random;

    public TraceSubstatRoller(PotentialOptionRegistry pool, RandomProvider random) {
        this.pool = pool;
        this.random = random;
    }

    public int substatCount(ItemGrade grade) {
        return COUNT.getOrDefault(grade == null ? ItemGrade.COMMON : grade, 1);
    }

    /** 주어진 등급으로 세부스탯 라인 목록을 롤한다(중복 옵션 없음). */
    public List<PotentialLine> roll(ItemGrade grade) {
        PotentialGrade potGrade = toPotentialGrade(grade);
        int count = substatCount(grade);

        List<PotentialOption> candidates = new ArrayList<>(pool.all().values().stream()
                .filter(option -> option.grade() == potGrade)
                .toList());
        if (candidates.isEmpty()) return List.of();

        List<PotentialLine> result = new ArrayList<>();
        int picks = Math.min(count, candidates.size());
        for (int i = 0; i < picks; i++) {
            PotentialOption opt = weightedPick(candidates);
            candidates.remove(opt); // 비복원
            double value = opt.valueMin() + random.nextDouble() * (opt.valueMax() - opt.valueMin());
            result.add(new PotentialLine(i + 1, potGrade, opt.optionCode(), value));
        }
        return result;
    }

    private PotentialOption weightedPick(List<PotentialOption> opts) {
        int total = 0;
        for (PotentialOption o : opts) total += Math.max(1, o.weight());
        int r = random.nextInt(total);
        int acc = 0;
        for (PotentialOption o : opts) {
            acc += Math.max(1, o.weight());
            if (r < acc) return o;
        }
        return opts.get(opts.size() - 1);
    }

    private static PotentialGrade toPotentialGrade(ItemGrade grade) {
        return switch (grade == null ? ItemGrade.COMMON : grade) {
            case COMMON    -> PotentialGrade.COMMON;
            case RARE      -> PotentialGrade.RARE;
            case EPIC      -> PotentialGrade.EPIC;
            case UNIQUE    -> PotentialGrade.UNIQUE;
            case LEGENDARY -> PotentialGrade.LEGENDARY;
        };
    }
}
