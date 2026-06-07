package com.poro.rpg.life.engine;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

public final class LifeSkillExpRegistry {
    private final Map<LifeType, TreeMap<Integer, LifeSkillExpRule>> values = new LinkedHashMap<>();

    public void register(LifeSkillExpRule rule) {
        values.computeIfAbsent(rule.lifeType(), ignored -> new TreeMap<>())
                .put(rule.level(), rule);
    }

    public Optional<LifeSkillExpRule> find(LifeType type, int level) {
        return Optional.ofNullable(values.getOrDefault(type, new TreeMap<>()).get(level));
    }

    public List<LifeSkillExpRule> rules(LifeType type) {
        TreeMap<Integer, LifeSkillExpRule> table = values.get(type);
        if (table == null) {
            return List.of();
        }
        return List.copyOf(table.values());
    }

    public int resolveLevel(LifeType type, long currentExp) {
        TreeMap<Integer, LifeSkillExpRule> table = values.get(type);
        if (table == null || table.isEmpty()) {
            return 1;
        }
        return table.values().stream()
                .filter(rule -> currentExp >= rule.cumulativeExp())
                .max(Comparator.comparingInt(LifeSkillExpRule::level))
                .map(LifeSkillExpRule::level)
                .orElse(1);
    }

    public Optional<LifeSkillExpRule> resolveRuleByExp(LifeType type, long currentExp) {
        TreeMap<Integer, LifeSkillExpRule> table = values.get(type);
        if (table == null || table.isEmpty()) {
            return Optional.empty();
        }
        return table.values().stream()
                .filter(rule -> currentExp >= rule.cumulativeExp())
                .max(Comparator.comparingInt(LifeSkillExpRule::level));
    }

    public int maxLevel(LifeType type) {
        TreeMap<Integer, LifeSkillExpRule> table = values.get(type);
        if (table == null || table.isEmpty()) {
            return 1;
        }
        return table.lastKey();
    }

    public Map<LifeType, TreeMap<Integer, LifeSkillExpRule>> all() {
        Map<LifeType, TreeMap<Integer, LifeSkillExpRule>> copy = new LinkedHashMap<>();
        values.forEach((type, table) -> copy.put(type, new TreeMap<>(table)));
        return Map.copyOf(copy);
    }

    public int size() {
        return values.values().stream().mapToInt(Map::size).sum();
    }
}
