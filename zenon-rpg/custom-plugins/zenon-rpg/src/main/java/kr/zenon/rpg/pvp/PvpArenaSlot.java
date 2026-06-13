package kr.zenon.rpg.pvp;

import org.bukkit.Location;

public final class PvpArenaSlot {
    private final int      id;
    private final Location spawnA;
    private final Location spawnB;
    private volatile String matchId;

    public PvpArenaSlot(int id, Location spawnA, Location spawnB) {
        this.id     = id;
        this.spawnA = spawnA;
        this.spawnB = spawnB;
    }

    public int      id()      { return id; }
    public Location spawnA()  { return spawnA.clone(); }
    public Location spawnB()  { return spawnB.clone(); }
    public boolean  isFree()  { return matchId == null; }
    public String   matchId() { return matchId; }

    synchronized boolean tryOccupy(String matchId) {
        if (this.matchId != null) return false;
        this.matchId = matchId;
        return true;
    }

    synchronized void release() { this.matchId = null; }
}
