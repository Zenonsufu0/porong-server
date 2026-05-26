package com.poro.empire.boss.room;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class BossRoomManager {
    private final Map<UUID, String>      rooms         = new ConcurrentHashMap<>();
    private final Map<UUID, String>      pendingBoss   = new ConcurrentHashMap<>();
    private final Map<UUID, Set<String>> clearedBosses = new ConcurrentHashMap<>();

    public boolean isInBossRoom(UUID uuid)         { return rooms.containsKey(uuid); }
    public void    enterRoom(UUID uuid, String id) { rooms.put(uuid, id); }
    public void    exitRoom(UUID uuid)             { rooms.remove(uuid); }

    public void            setPendingBoss(UUID uuid, String bossId)  { pendingBoss.put(uuid, bossId); }
    public Optional<String> getPendingBoss(UUID uuid)                { return Optional.ofNullable(pendingBoss.get(uuid)); }
    public void            clearPendingBoss(UUID uuid)               { pendingBoss.remove(uuid); }

    public void    markCleared(UUID uuid, String bossId)  { clearedBosses.computeIfAbsent(uuid, k -> ConcurrentHashMap.newKeySet()).add(bossId); }
    public boolean hasCleared(UUID uuid, String bossId)   { Set<String> s = clearedBosses.get(uuid); return s != null && s.contains(bossId); }
}
