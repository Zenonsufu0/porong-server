package kr.zenon.rpg.combat.engine;

import kr.zenon.rpg.common.registry.master.model.SkillMaster;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public final class TagDamageResolver {
    public ResolvedTagDamage resolve(SkillMaster skill, CombatUnitSnapshot source) {
        Map<String, Double> breakdown = new LinkedHashMap<>();
        appendTag(skill.tag1(), source, breakdown);
        appendTag(skill.tag2(), source, breakdown);

        double increased = 0.0d;
        for (double value : breakdown.values()) {
            increased += value;
        }
        return new ResolvedTagDamage(1.0d + increased, breakdown);
    }

    private void appendTag(String tagCode, CombatUnitSnapshot source, Map<String, Double> breakdown) {
        String normalized = normalize(tagCode);
        if (normalized.isBlank() || "-".equals(normalized)) {
            return;
        }
        breakdown.put(normalized, source.tagDamageIncrease(normalized));
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    public record ResolvedTagDamage(
            double multiplier,
            Map<String, Double> breakdown
    ) {
    }
}
