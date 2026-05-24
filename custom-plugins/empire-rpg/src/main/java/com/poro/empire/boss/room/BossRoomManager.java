package com.poro.empire.boss.room;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class BossRoomManager {
    private final Map<UUID, String> rooms = new ConcurrentHashMap<>();

    public boolean isInBossRoom(UUID uuid) {
        return rooms.containsKey(uuid);
    }

    public void enterRoom(UUID uuid, String bossId) {
        rooms.put(uuid, bossId);
    }

    public void exitRoom(UUID uuid) {
        rooms.remove(uuid);
    }
}
