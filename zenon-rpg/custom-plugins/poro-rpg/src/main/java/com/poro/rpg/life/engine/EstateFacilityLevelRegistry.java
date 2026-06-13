package com.poro.rpg.life.engine;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

public final class EstateFacilityLevelRegistry {
    private final Map<String, TreeMap<Integer, EstateFacilityLevelRule>> values = new LinkedHashMap<>();

    public void register(EstateFacilityLevelRule rule) {
        values.computeIfAbsent(normalize(rule.facilityId()), ignored -> new TreeMap<>())
                .put(rule.level(), rule);
    }

    public Optional<EstateFacilityLevelRule> find(String facilityId, int level) {
        TreeMap<Integer, EstateFacilityLevelRule> table = values.get(normalize(facilityId));
        if (table == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(table.get(level));
    }

    public List<EstateFacilityLevelRule> levels(String facilityId) {
        TreeMap<Integer, EstateFacilityLevelRule> table = values.get(normalize(facilityId));
        if (table == null) {
            return List.of();
        }
        return List.copyOf(table.values());
    }

    public Map<String, TreeMap<Integer, EstateFacilityLevelRule>> all() {
        Map<String, TreeMap<Integer, EstateFacilityLevelRule>> copy = new LinkedHashMap<>();
        values.forEach((id, rules) -> copy.put(id, new TreeMap<>(rules)));
        return Map.copyOf(copy);
    }

    public int size() {
        return values.values().stream().mapToInt(Map::size).sum();
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
