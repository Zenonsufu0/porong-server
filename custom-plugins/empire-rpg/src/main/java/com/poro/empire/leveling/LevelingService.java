package com.poro.empire.leveling;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class LevelingService {
    private final Map<UUID, Long> expByPlayer = new ConcurrentHashMap<>();

    public void addExp(UUID uuid, long exp) {
        if (exp <= 0) {
            return;
        }
        expByPlayer.merge(uuid, exp, Long::sum);
    }

    public int getLevel(UUID uuid) {
        long exp = expByPlayer.getOrDefault(uuid, 0L);
        return (int) Math.max(1L, (exp / 1000L) + 1L);
    }
}
