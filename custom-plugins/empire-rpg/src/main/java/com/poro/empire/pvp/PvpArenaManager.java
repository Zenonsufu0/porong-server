package com.poro.empire.pvp;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * PvP 아레나 방 풀 관리 (CANON §4).
 * <p>config.yml `pvp-room-slots` 섹션에서 슬롯 로드. 보스룸과 동일 패턴.
 */
public final class PvpArenaManager {

    private final List<PvpArenaSlot> slots;

    public PvpArenaManager(List<PvpArenaSlot> slots) {
        this.slots = Collections.unmodifiableList(new ArrayList<>(slots));
    }

    public static PvpArenaManager fromConfig(Plugin plugin) {
        List<PvpArenaSlot> loaded = new ArrayList<>();
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("pvp-room-slots");
        if (section == null) {
            plugin.getLogger().warning("[PvpArenaManager] pvp-room-slots 설정 없음 — 아레나 0개");
            return new PvpArenaManager(List.of());
        }
        for (String key : section.getKeys(false)) {
            ConfigurationSection s = section.getConfigurationSection(key);
            if (s == null) continue;
            int    id        = s.getInt("id");
            String worldName = s.getString("world", "world");
            World  world     = Bukkit.getWorld(worldName);
            if (world == null) {
                plugin.getLogger().warning("[PvpArenaManager] 월드 '" + worldName + "' 없음 — slot-" + id + " 건너뜀");
                continue;
            }
            ConfigurationSection sa = s.getConfigurationSection("spawn-a");
            ConfigurationSection sb = s.getConfigurationSection("spawn-b");
            if (sa == null || sb == null) continue;
            Location spawnA = new Location(world,
                    sa.getDouble("x"), sa.getDouble("y"), sa.getDouble("z"),
                    (float) sa.getDouble("yaw", 0.0), (float) sa.getDouble("pitch", 0.0));
            Location spawnB = new Location(world,
                    sb.getDouble("x"), sb.getDouble("y"), sb.getDouble("z"),
                    (float) sb.getDouble("yaw", 180.0), (float) sb.getDouble("pitch", 0.0));
            loaded.add(new PvpArenaSlot(id, spawnA, spawnB));
        }
        loaded.sort(Comparator.comparingInt(PvpArenaSlot::id));
        plugin.getLogger().info("[PvpArenaManager] " + loaded.size() + "개 아레나 슬롯 로드");
        return new PvpArenaManager(loaded);
    }

    /** 빈 슬롯 점유 시도. 실패 시 empty. */
    public Optional<PvpArenaSlot> tryAssign(String matchId) {
        for (PvpArenaSlot slot : slots) {
            if (slot.tryOccupy(matchId)) return Optional.of(slot);
        }
        return Optional.empty();
    }

    public void releaseByMatchId(String matchId) {
        slots.stream()
                .filter(s -> matchId.equals(s.matchId()))
                .findFirst()
                .ifPresent(PvpArenaSlot::release);
    }

    public Optional<PvpArenaSlot> findByMatchId(String matchId) {
        return slots.stream().filter(s -> matchId.equals(s.matchId())).findFirst();
    }

    public boolean isInArena(Location loc) {
        if (loc == null) return false;
        return slots.stream().anyMatch(slot -> sameRoom(slot, loc));
    }

    private boolean sameRoom(PvpArenaSlot slot, Location loc) {
        Location a = slot.spawnA();
        if (!a.getWorld().equals(loc.getWorld())) return false;
        // 20×20×20 박스 가정 (CANON 권장)
        return Math.abs(a.getX() - loc.getX()) < 25 && Math.abs(a.getZ() - loc.getZ()) < 25
                && Math.abs(a.getY() - loc.getY()) < 25;
    }

    public long freeCount()  { return slots.stream().filter(PvpArenaSlot::isFree).count(); }
    public int  totalCount() { return slots.size(); }
    public List<PvpArenaSlot> allSlots() { return slots; }
}
