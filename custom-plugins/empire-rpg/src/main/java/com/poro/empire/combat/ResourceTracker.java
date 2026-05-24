package com.poro.empire.combat;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ResourceTracker {
    private final Map<UUID, Integer> stacks = new ConcurrentHashMap<>();

    public int getStack(UUID uuid) {
        return stacks.getOrDefault(uuid, 0);
    }

    public void setStack(UUID uuid, int value) {
        if (value <= 0) {
            stacks.remove(uuid);
            return;
        }
        stacks.put(uuid, value);
    }

    public int incrementStack(UUID uuid, int max) {
        int next = Math.min(Math.max(1, max), getStack(uuid) + 1);
        stacks.put(uuid, next);
        return next;
    }

    public void resetStack(UUID uuid) {
        stacks.remove(uuid);
    }
}
