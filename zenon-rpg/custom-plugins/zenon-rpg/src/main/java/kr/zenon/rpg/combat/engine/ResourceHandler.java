package kr.zenon.rpg.combat.engine;

import kr.zenon.rpg.common.registry.master.model.SkillMaster;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class ResourceHandler {
    private static final Set<String> SUPPORTED_RESOURCES = Set.of(
            "crack",
            "sword_intent",
            "zanshin",
            "pressure",
            "resonance",
            "rune_piece",
            "combo_point",
            "shadow_mark"
    );

    private static final Map<String, Integer> RESOURCE_MAX_STACKS = Map.of(
            "crack", 10,
            "sword_intent", 5,
            "zanshin", 5,
            "pressure", 5,
            "resonance", 5,
            "rune_piece", 5,
            "combo_point", 5,
            "shadow_mark", 10
    );

    public ResourceMutationResult apply(
            SkillMaster skill,
            CombatUnitSnapshot source,
            CombatUnitSnapshot target,
            StateApplier stateApplier,
            BuffDebuffService buffDebuffService
    ) {
        List<String> events = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        String resourceType = normalized(skill.resourceType());
        if (resourceType.isBlank() || "none".equals(resourceType)) {
            return new ResourceMutationResult(events, warnings);
        }
        if (!SUPPORTED_RESOURCES.contains(resourceType)) {
            warnings.add("Unknown resource type: " + resourceType);
            return new ResourceMutationResult(events, warnings);
        }

        int gain = Math.max(0, skill.resourceGain());
        int cost = Math.max(0, skill.resourceCost());

        if (gain > 0) {
            int updated = source.addResource(resourceType, gain, RESOURCE_MAX_STACKS.getOrDefault(resourceType, 10));
            events.add("resource_gain:" + resourceType + " +" + gain + " => " + updated);
        }

        if (cost > 0) {
            int consumed = source.consumeResource(resourceType, cost);
            events.add("resource_cost:" + resourceType + " -" + consumed + " requested=" + cost);
            if (consumed < cost) {
                warnings.add("Insufficient resource: " + resourceType + " required=" + cost + " actual=" + consumed);
            }
        }

        if ("crack".equals(resourceType) && cost > 0) {
            int consumedCrackMark = buffDebuffService.consume(target.id(), "crack", cost);
            if (consumedCrackMark > 0) {
                events.add("state_consumed:crack -" + consumedCrackMark);
            }
        }

        if ("shadow_mark".equals(resourceType)) {
            events.addAll(applyShadowMarkHooks(skill, target, stateApplier, buffDebuffService));
        }

        return new ResourceMutationResult(events, warnings);
    }

    private List<String> applyShadowMarkHooks(
            SkillMaster skill,
            CombatUnitSnapshot target,
            StateApplier stateApplier,
            BuffDebuffService buffDebuffService
    ) {
        List<String> events = new ArrayList<>();
        int gain = Math.max(0, skill.resourceGain());
        int cost = Math.max(0, skill.resourceCost());

        for (int i = 0; i < gain; i++) {
            events.addAll(stateApplier.applyAny(target.id(), "shadow_mark"));
        }

        if (cost > 0) {
            int consumed = buffDebuffService.consume(target.id(), "shadow_mark", cost);
            if (consumed > 0) {
                events.add("state_consumed:shadow_mark -" + consumed);
            }
        }

        return events;
    }

    private String normalized(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    public record ResourceMutationResult(
            List<String> events,
            List<String> warnings
    ) {
    }
}
