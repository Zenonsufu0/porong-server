package com.poro.rpg.combat.engine;

import com.poro.rpg.common.registry.master.model.SkillMaster;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public final class ConditionalDamageResolver {
    public ResolvedConditionalDamage resolve(SkillMaster skill, SkillExecutionContext context) {
        String effectType = normalized(skill.secondaryEffectType());
        double bonus = parseBonus(skill.secondaryEffectValue());

        if (effectType.isBlank() || "none".equals(effectType)) {
            return new ResolvedConditionalDamage(1.0d, false, "NONE", bonus, List.of());
        }

        boolean activated = false;
        String reason = effectType;
        List<String> warnings = new ArrayList<>();

        switch (effectType) {
            case "bonus_vs_mark" -> activated = context.buffDebuffService().hasGroup(context.defender().id(), "DEBUFF_MARK");
            case "bonus_vs_status" -> activated = context.buffDebuffService().hasGroup(context.defender().id(), "DEBUFF_STATUS");
            case "bonus_on_field" -> activated = context.flagEnabled("on_field");
            case "bonus_on_rune_field" -> activated = context.flagEnabled("rune_field_active");
            case "bonus_if_resource_at_least" -> {
                String value = normalized(skill.secondaryEffectValue());
                String[] tokens = value.split("[:=]");
                if (tokens.length == 2) {
                    String resource = normalized(tokens[0]);
                    int required = parseInt(tokens[1], 0);
                    activated = context.attacker().resource(resource) >= required;
                    reason = effectType + "(" + resource + ">=" + required + ")";
                } else {
                    warnings.add("Invalid bonus_if_resource_at_least format: " + skill.secondaryEffectValue());
                }
            }
            default -> warnings.add("Unknown conditional effect type: " + skill.secondaryEffectType());
        }

        return new ResolvedConditionalDamage(
                activated ? 1.0d + bonus : 1.0d,
                activated,
                reason,
                bonus,
                warnings
        );
    }

    public List<String> applyPostHitHooks(SkillMaster skill, SkillExecutionContext context) {
        if (!context.allowImmediateStateConsume(skill.skillId())) {
            return List.of();
        }

        String effectType = normalized(skill.secondaryEffectType());
        List<String> events = new ArrayList<>();
        if ("bonus_vs_mark".equals(effectType)) {
            int consumed = consumeFirstAvailableMark(context);
            if (consumed > 0) {
                events.add("post_hit_consume:mark -" + consumed);
            }
        } else if ("bonus_vs_status".equals(effectType)) {
            Optional<String> firstStatus = context.buffDebuffService()
                    .findFirstStateCodeByGroup(context.defender().id(), "DEBUFF_STATUS");
            if (firstStatus.isPresent()) {
                int consumed = context.buffDebuffService().consume(context.defender().id(), firstStatus.get(), 1);
                if (consumed > 0) {
                    events.add("post_hit_consume:" + firstStatus.get() + " -" + consumed);
                }
            }
        }
        return events;
    }

    private int consumeFirstAvailableMark(SkillExecutionContext context) {
        String[] priority = {"shadow_mark", "target_mark", "crack", "exposed_weakness", "stigma"};
        for (String stateCode : priority) {
            int consumed = context.buffDebuffService().consume(context.defender().id(), stateCode, 1);
            if (consumed > 0) {
                return consumed;
            }
        }
        return 0;
    }

    private double parseBonus(String raw) {
        if (raw == null || raw.isBlank()) {
            return 0.0d;
        }
        try {
            double parsed = Double.parseDouble(raw.trim());
            return parsed > 1.0d ? parsed / 100.0d : parsed;
        } catch (NumberFormatException ignored) {
            return 0.0d;
        }
    }

    private int parseInt(String raw, int fallback) {
        try {
            return Integer.parseInt(raw.trim());
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private String normalized(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    public record ResolvedConditionalDamage(
            double multiplier,
            boolean activated,
            String reason,
            double bonus,
            List<String> warnings
    ) {
    }
}
