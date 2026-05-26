package com.poro.empire.boss.room;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.Plugin;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class BossRoomManager {

    private final List<BossRoomSlot>     slots;
    private final Map<UUID, Integer>     playerToSlot  = new ConcurrentHashMap<>();
    private final Map<Integer, String>   slotToBoss    = new ConcurrentHashMap<>();
    private final Map<UUID, String>      pendingBoss   = new ConcurrentHashMap<>();
    private final Map<UUID, Set<String>> clearedBosses = new ConcurrentHashMap<>();

    public BossRoomManager(List<BossRoomSlot> slots) {
        this.slots = Collections.unmodifiableList(new ArrayList<>(slots));
    }

    /** config.yml boss-room-slots 섹션으로부터 슬롯 로드. */
    public static BossRoomManager fromConfig(Plugin plugin) {
        List<BossRoomSlot> loaded = new ArrayList<>();
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("boss-room-slots");
        if (section == null) {
            plugin.getLogger().warning("[BossRoomManager] boss-room-slots 설정 없음 — 보스룸 0개");
            return new BossRoomManager(List.of());
        }
        for (String key : section.getKeys(false)) {
            ConfigurationSection s = section.getConfigurationSection(key);
            if (s == null) continue;
            int    id        = s.getInt("id");
            String worldName = s.getString("world", "world");
            World  world     = Bukkit.getWorld(worldName);
            if (world == null) {
                plugin.getLogger().warning("[BossRoomManager] 월드 '" + worldName + "' 없음 — slot-" + id + " 건너뜀");
                continue;
            }
            ConfigurationSection ps = s.getConfigurationSection("player-spawn");
            ConfigurationSection bs = s.getConfigurationSection("boss-spawn");
            if (ps == null || bs == null) continue;
            Location playerSpawn = new Location(world,
                    ps.getDouble("x"), ps.getDouble("y"), ps.getDouble("z"),
                    (float) ps.getDouble("yaw", 180.0), (float) ps.getDouble("pitch", 0.0));
            Location bossSpawn = new Location(world,
                    bs.getDouble("x"), bs.getDouble("y"), bs.getDouble("z"));
            loaded.add(new BossRoomSlot(id, playerSpawn, bossSpawn));
        }
        loaded.sort(Comparator.comparingInt(BossRoomSlot::id));
        plugin.getLogger().info("[BossRoomManager] " + loaded.size() + "개 보스룸 슬롯 로드");
        return new BossRoomManager(loaded);
    }

    // ── 방 배정 ──────────────────────────────────────────────────────

    /** 빈 슬롯을 찾아 리더에게 배정. 방이 꽉 차면 empty 반환. */
    public Optional<BossRoomSlot> assignRoom(UUID leaderId, String bossId) {
        for (BossRoomSlot slot : slots) {
            if (slot.tryOccupy(leaderId)) {
                slotToBoss.put(slot.id(), bossId);
                return Optional.of(slot);
            }
        }
        return Optional.empty();
    }

    public void              enterRoom(UUID uuid, int slotId) { playerToSlot.put(uuid, slotId); }
    public void              exitRoom(UUID uuid)              { playerToSlot.remove(uuid); }
    public boolean           isInBossRoom(UUID uuid)          { return playerToSlot.containsKey(uuid); }
    public Optional<Integer> slotOf(UUID uuid)               { return Optional.ofNullable(playerToSlot.get(uuid)); }
    public Optional<String>  bossInSlot(int slotId)          { return Optional.ofNullable(slotToBoss.get(slotId)); }

    public void releaseSlot(int slotId) {
        slots.stream().filter(s -> s.id() == slotId).findFirst().ifPresent(BossRoomSlot::release);
        slotToBoss.remove(slotId);
    }

    public long  freeCount()  { return slots.stream().filter(BossRoomSlot::isFree).count(); }
    public int   totalCount() { return slots.size(); }
    public List<BossRoomSlot> allSlots() { return slots; }

    // ── 보스 선택 / 클리어 추적 ──────────────────────────────────────

    public void             setPendingBoss(UUID uuid, String bossId) { pendingBoss.put(uuid, bossId); }
    public Optional<String> getPendingBoss(UUID uuid)                { return Optional.ofNullable(pendingBoss.get(uuid)); }
    public void             clearPendingBoss(UUID uuid)              { pendingBoss.remove(uuid); }

    public void    markCleared(UUID uuid, String bossId) { clearedBosses.computeIfAbsent(uuid, k -> ConcurrentHashMap.newKeySet()).add(bossId); }
    public boolean hasCleared(UUID uuid, String bossId)  { Set<String> s = clearedBosses.get(uuid); return s != null && s.contains(bossId); }
}
