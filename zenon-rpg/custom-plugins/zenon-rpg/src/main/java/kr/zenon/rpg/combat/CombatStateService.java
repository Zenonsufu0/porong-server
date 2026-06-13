package kr.zenon.rpg.combat;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class CombatStateService {

    private static final long COMBAT_DURATION_MS = 8_000L;

    private final Map<UUID, Long> lastHitTimes = new ConcurrentHashMap<>();

    public boolean isInCombat(UUID uuid) {
        Long lastHit = lastHitTimes.get(uuid);
        if (lastHit == null) return false;
        if (System.currentTimeMillis() - lastHit > COMBAT_DURATION_MS) {
            lastHitTimes.remove(uuid);
            return false;
        }
        return true;
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
