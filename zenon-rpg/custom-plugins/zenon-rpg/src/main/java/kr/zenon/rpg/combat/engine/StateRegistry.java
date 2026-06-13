package kr.zenon.rpg.combat.engine;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class StateRegistry {
    private final Map<String, StateDefinition> definitions = new ConcurrentHashMap<>();

    public void register(StateDefinition definition) {
        definitions.put(normalize(definition.stateCode()), definition);
    }

    public Optional<StateDefinition> find(String stateCode) {
        return Optional.ofNullable(definitions.get(normalize(stateCode)));
    }

    public boolean contains(String stateCode) {
        return find(stateCode).isPresent();
    }

    public Map<String, StateDefinition> all() {
        return Map.copyOf(new LinkedHashMap<>(definitions));
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
