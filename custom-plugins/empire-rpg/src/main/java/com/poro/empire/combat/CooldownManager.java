package com.poro.empire.combat;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class CooldownManager {
    // long[] = { expireAt (epoch ms), totalMs }
    private final Map<UUID, Map<String, long[]>> cooldowns = new ConcurrentHashMap<>();

    public boolean isOnCooldown(UUID playerId, String key) {
        return getRemainingMillis(playerId, key) > 0;
    }

    public long getRemainingMillis(UUID playerId, String key) {
        long[] entry = getEntry(playerId, key);
        if (entry == null) return 0L;
        long remaining = entry[0] - System.currentTimeMillis();
        if (remaining <= 0) {
            removeEntry(playerId, key);
            return 0L;
        }
        return remaining;
    }

    public long getTotalMillis(UUID playerId, String key) {
        long[] entry = getEntry(playerId, key);
        return entry == null ? 0L : entry[1];
    }

    public void applyCooldown(UUID playerId, String key, Duration duration) {
        long totalMs = duration.toMillis();
        long expireAt = System.currentTimeMillis() + totalMs;
        cooldowns.computeIfAbsent(playerId, ignored -> new ConcurrentHashMap<>())
                 .put(key.toLowerCase(), new long[]{expireAt, totalMs});
    }

    public void cancelCooldown(UUID playerId, String key) {
        removeEntry(playerId, key);
    }

    public static String formatSeconds(long millis) {
        return String.format("%.1f", millis / 1000.0);
    }

    private long[] getEntry(UUID playerId, String key) {
        Map<String, long[]> playerMap = cooldowns.get(playerId);
        return playerMap == null ? null : playerMap.get(key.toLowerCase());
    }

    private void removeEntry(UUID playerId, String key) {
        Map<String, long[]> playerMap = cooldowns.get(playerId);
        if (playerMap == null) return;
        playerMap.remove(key.toLowerCase());
        if (playerMap.isEmpty()) cooldowns.remove(playerId);
    }
}
