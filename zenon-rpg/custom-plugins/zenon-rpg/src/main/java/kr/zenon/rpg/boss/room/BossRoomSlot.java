package kr.zenon.rpg.boss.room;

import org.bukkit.Location;
import org.bukkit.util.BoundingBox;
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

    /**
     * 방 영역 박스 — 슬롯 해제 시 이 안의 모든 몹/소환수를 청소하는 데 사용 (DL-129 추가#23).
     * 슬롯 X 간격 60 → X 반경 28(이웃 침범 방지). z는 player/boss 스폰을 감싸도록 ±35, y는 −20~+30.
     */
    public BoundingBox roomBox() {
        double cx = bossSpawn.getX();
        double cz = (playerSpawn.getZ() + bossSpawn.getZ()) / 2.0;
        double cy = bossSpawn.getY();
        return new BoundingBox(cx - 28, cy - 20, cz - 35, cx + 28, cy + 30, cz + 35);
    }
    public boolean  isFree()       { return occupyingLeader == null; }
    public Optional<UUID> leader() { return Optional.ofNullable(occupyingLeader); }

    synchronized boolean tryOccupy(UUID leader) {
        if (occupyingLeader != null) return false;
        occupyingLeader = leader;
        return true;
    }

    synchronized void release() { occupyingLeader = null; }
}
