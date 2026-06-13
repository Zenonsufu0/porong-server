package kr.zenon.rpg.life.engine;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public final class EstateUnlockRuleRegistry {
    private final Map<String, EstateUnlockRule> values = new LinkedHashMap<>();

    public void register(EstateUnlockRule rule) {
        values.put(normalize(rule.estateId()), rule);
    }

    public Optional<EstateUnlockRule> find(String estateId) {
        return Optional.ofNullable(values.get(normalize(estateId)));
    }

    public Map<String, EstateUnlockRule> all() {
        return Map.copyOf(new LinkedHashMap<>(values));
    }

    public int size() {
        return values.size();
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
