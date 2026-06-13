package kr.zenon.rpg.growth.engine;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public final class GrowthStatBlock {
    private final Map<String, Double> values = new LinkedHashMap<>();
    private final Set<String> flags = new LinkedHashSet<>();

    public void add(String effectType, double amount) {
        if (effectType == null || effectType.isBlank()) {
            return;
        }
        values.merge(effectType.trim().toLowerCase(), amount, Double::sum);
    }

    public void addFlag(String flagCode) {
        if (flagCode == null || flagCode.isBlank()) {
            return;
        }
        flags.add(flagCode.trim().toLowerCase());
    }

    public void merge(GrowthStatBlock other) {
        if (other == null) {
            return;
        }
        other.values.forEach(this::add);
        other.flags.forEach(this::addFlag);
    }

    public Map<String, Double> values() {
        return Map.copyOf(new LinkedHashMap<>(values));
    }

    public Set<String> flags() {
        return Set.copyOf(new LinkedHashSet<>(flags));
    }
}
