package com.poro.rpg.growth.engine;

import com.poro.rpg.common.registry.InMemoryRegistry;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public final class RuneRegistry {
    private final InMemoryRegistry<String, RuneMaster> delegate = new InMemoryRegistry<>();

    public void register(RuneMaster runeMaster) {
        delegate.register(runeMaster.runeId(), runeMaster);
    }

    public Optional<RuneMaster> find(String runeId) {
        return delegate.find(normalize(runeId));
    }

    public Map<String, RuneMaster> all() {
        return delegate.snapshot();
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
