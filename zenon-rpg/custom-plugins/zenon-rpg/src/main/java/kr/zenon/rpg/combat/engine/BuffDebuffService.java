package kr.zenon.rpg.combat.engine;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class BuffDebuffService {
    private final StateRegistry stateRegistry;
    private final Map<String, Map<String, Integer>> stateStacksByUnit = new ConcurrentHashMap<>();

    public BuffDebuffService(StateRegistry stateRegistry) {
        this.stateRegistry = stateRegistry;
    }

    public int apply(String targetUnitId, StateDefinition definition, int stacks) {
        if (stacks <= 0) {
            return stackOf(targetUnitId, definition.stateCode());
        }

        Map<String, Integer> states = stateStacksByUnit.computeIfAbsent(targetUnitId, ignored -> new ConcurrentHashMap<>());
        int current = states.getOrDefault(definition.stateCode(), 0);
        int maxStack = Math.max(1, definition.maxStack());
        int applied = definition.stackable()
                ? Math.min(maxStack, current + stacks)
                : Math.min(maxStack, Math.max(1, current + 1));
        states.put(definition.stateCode(), applied);
        return applied;
    }

    public int consume(String targetUnitId, String stateCode, int amount) {
        if (amount <= 0) {
            return 0;
        }
        String normalized = normalize(stateCode);
        Map<String, Integer> states = stateStacksByUnit.get(targetUnitId);
        if (states == null) {
            return 0;
        }
        int current = states.getOrDefault(normalized, 0);
        int consumed = Math.min(current, amount);
        int updated = Math.max(0, current - consumed);
        if (updated == 0) {
            states.remove(normalized);
        } else {
            states.put(normalized, updated);
        }
        return consumed;
    }

    public boolean hasState(String targetUnitId, String stateCode) {
        return stackOf(targetUnitId, stateCode) > 0;
    }

    public int stackOf(String targetUnitId, String stateCode) {
        Map<String, Integer> states = stateStacksByUnit.get(targetUnitId);
        if (states == null) {
            return 0;
        }
        return states.getOrDefault(normalize(stateCode), 0);
    }

    public boolean hasGroup(String targetUnitId, String stateGroup) {
        Map<String, Integer> states = stateStacksByUnit.get(targetUnitId);
        if (states == null || states.isEmpty()) {
            return false;
        }
        String normalizedGroup = normalizeGroup(stateGroup);
        for (Map.Entry<String, Integer> entry : states.entrySet()) {
            if (entry.getValue() <= 0) {
                continue;
            }
            Optional<StateDefinition> definition = stateRegistry.find(entry.getKey());
            if (definition.isPresent() && normalizedGroup.equals(definition.get().stateGroup())) {
                return true;
            }
        }
        return false;
    }

    public Optional<String> findFirstStateCodeByGroup(String targetUnitId, String stateGroup) {
        Map<String, Integer> states = stateStacksByUnit.get(targetUnitId);
        if (states == null || states.isEmpty()) {
            return Optional.empty();
        }
        String normalizedGroup = normalizeGroup(stateGroup);
        for (Map.Entry<String, Integer> entry : states.entrySet()) {
            if (entry.getValue() <= 0) {
                continue;
            }
            Optional<StateDefinition> definition = stateRegistry.find(entry.getKey());
            if (definition.isPresent() && normalizedGroup.equals(definition.get().stateGroup())) {
                return Optional.of(entry.getKey());
            }
        }
        return Optional.empty();
    }

    public double additionalDamageReduction(String targetUnitId) {
        int stack = stackOf(targetUnitId, "damage_reduction");
        if (stack <= 0) {
            return 0.0d;
        }
        return Math.min(0.5d, stack * 0.03d);
    }

    public Map<String, Integer> snapshot(String targetUnitId) {
        Map<String, Integer> states = stateStacksByUnit.get(targetUnitId);
        if (states == null) {
            return Map.of();
        }
        return Map.copyOf(new LinkedHashMap<>(states));
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeGroup(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }
}
