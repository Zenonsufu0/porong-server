package com.poro.rpg.common.registry;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryRegistry<K, V> implements Registry<K, V> {
    private final Map<K, V> values = new ConcurrentHashMap<>();

    @Override
    public void register(K key, V value) {
        values.put(Objects.requireNonNull(key, "key"), Objects.requireNonNull(value, "value"));
    }

    @Override
    public Optional<V> find(K key) {
        return Optional.ofNullable(values.get(key));
    }

    @Override
    public Map<K, V> snapshot() {
        return Map.copyOf(new LinkedHashMap<>(values));
    }
}
