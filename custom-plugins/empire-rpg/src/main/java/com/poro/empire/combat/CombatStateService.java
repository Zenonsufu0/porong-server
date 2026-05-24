package com.poro.empire.combat;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class CombatStateService {
    private final Map<UUID, Long> lastHitTimes = new ConcurrentHashMap<>();

    public boolean isInCombat(UUID uuid) {
        return lastHitTimes.containsKey(uuid);
    }

    public void enterCombat(UUID uuid) {
        lastHitTimes.put(uuid, System.currentTimeMillis());
    }

    public void exitCombat(UUID uuid) {
        lastHitTimes.remove(uuid);
    }

    public long getLastHitTime(UUID uuid) {
        return lastHitTimes.getOrDefault(uuid, 0L);
    }
}
