package com.poro.rpg.growth.engine;

import com.poro.rpg.common.registry.InMemoryRegistry;

import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class PotentialOptionRegistry {
    private final InMemoryRegistry<String, PotentialOption> delegate = new InMemoryRegistry<>();

    public void register(PotentialOption option) {
        delegate.register(option.poolId() + ":" + option.optionCode() + ":" + option.grade().name(), option);
    }

    public List<PotentialOption> findByPoolAndGrade(String poolId, PotentialGrade grade) {
        String normalizedPool = normalize(poolId);
        return delegate.snapshot().values().stream()
                .filter(option -> option.poolId().equals(normalizedPool))
                .filter(option -> option.grade() == grade)
                .toList();
    }

    public Map<String, PotentialOption> all() {
        return delegate.snapshot();
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
