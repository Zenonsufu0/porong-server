package kr.zenon.rpg.growth.engine;

import kr.zenon.rpg.common.registry.InMemoryRegistry;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public final class EngravingRegistry {
    private final InMemoryRegistry<String, EngravingMaster> delegate = new InMemoryRegistry<>();

    public void register(EngravingMaster engravingMaster) {
        delegate.register(engravingMaster.engravingId(), engravingMaster);
    }

    public Optional<EngravingMaster> find(String engravingId) {
        return delegate.find(normalize(engravingId));
    }

    public Map<String, EngravingMaster> all() {
        return delegate.snapshot();
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
