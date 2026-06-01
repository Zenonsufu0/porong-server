package com.poro.rpg.boss.room;

import org.bukkit.Location;
import java.util.Optional;
import java.util.UUID;

public final class BossRoomSlot {
    private final int      id;
    private final Location playerSpawn;
    private final Location bossSpawn;
    private volatile UUID  occupyingLeader;

    public BossRoomSlot(int id, Location playerSpawn, Location bossSpawn) {
        this.id          = id;
        this.playerSpawn = playerSpawn;
        this.bossSpawn   = bossSpawn;
    }

    public int      id()           { return id; }
    public Location playerSpawn()  { return playerSpawn.clone(); }
    public Location bossSpawn()    { return bossSpawn.clone(); }
    public boolean  isFree()       { return occupyingLeader == null; }
    public Optional<UUID> leader() { return Optional.ofNullable(occupyingLeader); }

    synchronized boolean tryOccupy(UUID leader) {
        if (occupyingLeader != null) return false;
        occupyingLeader = leader;
        return true;
    }

    synchronized void release() { occupyingLeader = null; }
}
