package com.poro.empire.growth.island;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class IslandTerritoryStateStore {
    private final Map<UUID, IslandTerritoryState> territories = new ConcurrentHashMap<>();

    public IslandTerritoryState getOrCreate(UUID uuid) {
        return getOrCreate(uuid, "플레이어");
    }

    public IslandTerritoryState getOrCreate(UUID uuid, String playerName) {
        return territories.computeIfAbsent(uuid, key -> new IslandTerritoryState(playerName));
    }

    public Optional<IslandTerritoryState> get(UUID uuid) {
        return Optional.ofNullable(territories.get(uuid));
    }

    public void put(UUID uuid, IslandTerritoryState state) {
        territories.put(uuid, state);
    }

    public void remove(UUID uuid) {
        territories.remove(uuid);
    }
}
