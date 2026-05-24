package com.poro.empire.growth.island;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class IslandStorageStore {
    private final Map<UUID, IslandStorage> storages = new ConcurrentHashMap<>();

    public IslandStorage getOrCreate(UUID uuid) {
        return storages.computeIfAbsent(uuid, key -> new IslandStorage());
    }

    public Optional<IslandStorage> get(UUID uuid) {
        return Optional.ofNullable(storages.get(uuid));
    }

    public void put(UUID uuid, IslandStorage storage) {
        storages.put(uuid, storage);
    }
}
