package kr.poro.poromoncore.config;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * legendary_pools.json 매핑 (encounter_pool_design.md, 결정 018~026).
 * 조우권 등급/컨셉별 소환 풀(검증된 cobblemon:<id> + 가중치). draft.yml 변환 번들.
 */
public class EncounterConfig {
    public int configVersion = 1;
    public Map<String, Pool> pools = Map.of();

    public static class Pool {
        public String type;                 // rare/basic/intermediate/advanced/apex/field_event/theme
        public String displayNameKo;
        public Map<String, Integer> stageWeight; // 2단계 가중: 희귀=basic/middle/final 70/20/10, 컨셉=intermediate/advanced/apex 55/35/10
        public List<Candidate> candidates = List.of();
    }

    public static class Candidate {
        public String species;              // cobblemon:<id>
        public String displayNameKo;
        public String stage;                // 가중 키: 희귀 basic/middle/final · 컨셉 intermediate/advanced/apex
        public int weight = 1;
        public boolean enabled = true;
    }

    // ── 2단계 가중 (단일 진실 공급원: 추첨·확률표시 양쪽이 이걸 사용) ──────────────
    //
    // stageWeight가 있으면: P(후보) = (stage 유효비중 / Σ유효비중) × (후보 weight / Σ동일 stage weight).
    //   - stage 유효비중 = stageWeight를 B-cap 재분배: 후보 없는 stage의 비중은 "가장 높은 비중의
    //     존재 stage"가 흡수 → 컨셉의 최상급(apex 10)은 항상 ≤10% 유지(고정 cap). 희귀(70/20/10)는
    //     전 stage 상시 존재라 재분배 없음.
    // stageWeight가 없으면: 후보 weight 직접 비례(단일 등급 풀).

    public static List<Candidate> enabled(Pool pool) {
        List<Candidate> out = new ArrayList<>();
        if (pool == null) return out;
        for (Candidate c : pool.candidates) if (c.enabled && c.weight > 0) out.add(c);
        return out;
    }

    /** 후보 → 확률(0~1). 입력 순서(풀 정의 순서)를 보존. */
    public static Map<Candidate, Double> probabilities(Pool pool) {
        Map<Candidate, Double> out = new LinkedHashMap<>();
        List<Candidate> en = enabled(pool);
        if (en.isEmpty()) return out;

        if (pool.stageWeight == null || pool.stageWeight.isEmpty()) {
            double tot = 0;
            for (Candidate c : en) tot += c.weight;
            for (Candidate c : en) out.put(c, tot > 0 ? c.weight / tot : 0);
            return out;
        }

        // stage별 그룹 + stage별 weight 합
        Map<String, Double> stageWsum = new LinkedHashMap<>();
        for (Candidate c : en) {
            String s = c.stage == null ? "" : c.stage;
            stageWsum.merge(s, (double) c.weight, Double::sum);
        }
        Map<String, Double> eff = effectiveStageWeights(pool.stageWeight, stageWsum.keySet());
        double swTot = 0;
        for (double v : eff.values()) swTot += v;

        for (Candidate c : en) {
            String s = c.stage == null ? "" : c.stage;
            double stageShare = swTot > 0 ? eff.getOrDefault(s, 0.0) / swTot : 0;
            double wsum = stageWsum.getOrDefault(s, 0.0);
            out.put(c, wsum > 0 ? stageShare * (c.weight / wsum) : 0);
        }
        return out;
    }

    /** B-cap: 후보 없는 stage 비중을 "비중이 가장 큰 존재 stage"가 흡수. */
    private static Map<String, Double> effectiveStageWeights(Map<String, Integer> canonical, java.util.Set<String> present) {
        Map<String, Double> eff = new LinkedHashMap<>();
        String floor = null;
        double floorW = -1;
        for (String s : present) {
            double w = canonical.getOrDefault(s, 0).doubleValue();
            eff.put(s, w);
            if (w > floorW) { floorW = w; floor = s; }
        }
        double absent = 0;
        for (Map.Entry<String, Integer> e : canonical.entrySet())
            if (!present.contains(e.getKey())) absent += e.getValue();
        if (floor != null) eff.put(floor, eff.get(floor) + absent);
        return eff;
    }

    /** 가중 추첨. r∈[0,1). probabilities()와 동일 분포. */
    public static Candidate weightedPick(Pool pool, double r) {
        Map<Candidate, Double> probs = probabilities(pool);
        if (probs.isEmpty()) return null;
        double acc = 0;
        Candidate last = null;
        for (Map.Entry<Candidate, Double> e : probs.entrySet()) {
            acc += e.getValue();
            last = e.getKey();
            if (r < acc) return e.getKey();
        }
        return last;
    }
}
