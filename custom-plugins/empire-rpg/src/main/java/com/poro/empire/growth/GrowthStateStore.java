package com.poro.empire.growth;

import com.poro.empire.growth.engine.PlayerGrowthState;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class GrowthStateStore {
    private final Map<UUID, PlayerGrowthState> states = new ConcurrentHashMap<>();

    public PlayerGrowthState getOrCreate(UUID uuid, String classId) {
        return states.computeIfAbsent(uuid, key -> new PlayerGrowthState(key.toString(), classId));
    }

    public Optional<PlayerGrowthState> get(UUID uuid) {
        return Optional.ofNullable(states.get(uuid));
    }

    public void put(UUID uuid, PlayerGrowthState state) {
        states.put(uuid, state);
    }

    public void remove(UUID uuid) {
        states.remove(uuid);
    }
}
