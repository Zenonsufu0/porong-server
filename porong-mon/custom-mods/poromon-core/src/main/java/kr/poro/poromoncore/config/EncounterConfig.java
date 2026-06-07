package kr.poro.poromoncore.config;

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
        public Map<String, Integer> stageWeight; // 희귀 풀만(basic/middle/final)
        public List<Candidate> candidates = List.of();
    }

    public static class Candidate {
        public String species;              // cobblemon:<id>
        public String displayNameKo;
        public String stage;                // 희귀 풀: basic/middle/final
        public int weight = 1;
        public boolean enabled = true;
    }
}
