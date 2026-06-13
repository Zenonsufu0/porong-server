package kr.zenon.rpg.life.engine;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public final class LifeGatherNodeRegistry {
    private final Map<String, LifeGatherNode> values = new LinkedHashMap<>();

    public void register(LifeGatherNode node) {
        values.put(normalize(node.gatherId()), node);
    }

    public Optional<LifeGatherNode> find(String gatherId) {
        return Optional.ofNullable(values.get(normalize(gatherId)));
    }

    public Map<String, LifeGatherNode> all() {
        return Map.copyOf(new LinkedHashMap<>(values));
    }

    public int size() {
        return values.size();
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
