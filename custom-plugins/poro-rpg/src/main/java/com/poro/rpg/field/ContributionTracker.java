package com.poro.rpg.field;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ContributionTracker {
    private final Map<UUID, ConcurrentHashMap<UUID, Double>> data = new ConcurrentHashMap<>();

    public void recordDamage(UUID entityId, UUID playerId, double damage) {
        if (damage <= 0) return;
        data.computeIfAbsent(entityId, k -> new ConcurrentHashMap<>())
            .merge(playerId, damage, Double::sum);
    }

    public Map<UUID, Double> finalizeShares(UUID entityId) {
        ConcurrentHashMap<UUID, Double> damages = data.remove(entityId);
        if (damages == null || damages.isEmpty()) return Map.of();
        double total = damages.values().stream().mapToDouble(Double::doubleValue).sum();
        if (total == 0) return Map.of();
        Map<UUID, Double> shares = new LinkedHashMap<>();
        damages.forEach((uuid, dmg) -> shares.put(uuid, dmg / total * 100.0));
        return Map.copyOf(shares);
    }

    public void evict(UUID entityId) {
        data.remove(entityId);
    }
}
