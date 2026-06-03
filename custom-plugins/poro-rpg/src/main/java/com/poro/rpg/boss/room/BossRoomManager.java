package com.poro.rpg.boss.room;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.Plugin;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class BossRoomManager {

    private final List<BossRoomSlot>     slots;
    private final Map<UUID, Integer>     playerToSlot  = new ConcurrentHashMap<>();
    private final Map<Integer, String>   slotToBoss    = new ConcurrentHashMap<>();
    private final Map<String, Integer>   runToSlot     = new ConcurrentHashMap<>();
    private final Map<Integer, String>   slotToRunId   = new ConcurrentHashMap<>();
    private final Map<UUID, String>      pendingBoss   = new ConcurrentHashMap<>();
    private final Map<UUID, Set<String>> clearedBosses = new ConcurrentHashMap<>();
    // 공유 부활 토큰(데스카운트) — slotId → [remaining, max]. 1/2/3인 = 3/4/5 (partySize+2).
    private final Map<Integer, int[]>    slotDeaths    = new ConcurrentHashMap<>();
    // 영속 클리어 복원 (DL-097): boss_session에서 lazy 로드, 플레이어당 1회 캐시.
    private Function<UUID, Set<String>> clearSource;
    private final Set<UUID> clearsLoaded = ConcurrentHashMap.newKeySet();

    public BossRoomManager(List<BossRoomSlot> slots) {
        this.slots = Collections.unmodifiableList(new ArrayList<>(slots));
    }

    /** 클리어 영속 소스 주입 (uuid → 클리어한 boss_id 집합). PoroRPGPlugin 와이어링. */
    public void attachClearSource(Function<UUID, Set<String>> source) {
        this.clearSource = source;
    }

    /** 게이트 첫 조회 시 boss_session에서 클리어 기록을 in-memory로 복원 (플레이어당 1회). */
    private void ensureClearsLoaded(UUID uuid) {
        if (clearSource == null || uuid == null || !clearsLoaded.add(uuid)) return;
        Set<String> persisted = clearSource.apply(uuid);
        if (persisted != null && !persisted.isEmpty()) {
            clearedBosses.computeIfAbsent(uuid, k -> ConcurrentHashMap.newKeySet()).addAll(persisted);
        }
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
    public boolean           isInBossRoom(UUID uuid)          { return playerToSlot.containsKey(uuid); }
    public Optional<Integer> slotOf(UUID uuid)               { return Optional.ofNullable(playerToSlot.get(uuid)); }
    public Optional<String>  bossInSlot(int slotId)          { return Optional.ofNullable(slotToBoss.get(slotId)); }

    /** 플레이어가 방에서 나갈 때 호출. 마지막 인원이 나가면 슬롯 자동 해제. */
    public void exitRoom(UUID uuid) {
        Integer slotId = playerToSlot.remove(uuid);
        if (slotId != null && !playerToSlot.containsValue(slotId)) {
            String runId = slotToRunId.remove(slotId);
            if (runId != null) runToSlot.remove(runId);
            releaseSlot(slotId);
        }
    }

    /** startRun() 성공 후 runId ↔ slotId 매핑 등록. */
    public void registerRun(String runId, int slotId) {
        runToSlot.put(runId, slotId);
        slotToRunId.put(slotId, runId);
    }

    /** slotId → runId 역조회 (파티 전멸 시 endRun 호출용). */
    public Optional<String> runIdOf(int slotId) {
        return Optional.ofNullable(slotToRunId.get(slotId));
    }

    // ── 공유 부활 토큰(데스카운트) ──────────────────────────────────────

    /** 입장 시 파티 인원수 기반으로 공유 부활 토큰을 초기화한다. 1/2/3인 → 3/4/5 (partySize+2). */
    public void initDeathPool(int slotId, int partySize) {
        int tokens = Math.max(1, partySize) + 2;
        slotDeaths.put(slotId, new int[]{tokens, tokens});
    }

    /**
     * 보스룸 사망 1건을 처리한다.
     * 토큰이 남아 있으면 1 소모 후 {@code true}(보스룸 리스폰), 없으면 {@code false}(전멸·런 종료).
     */
    public boolean consumeDeath(int slotId) {
        int[] pool = slotDeaths.get(slotId);
        if (pool == null || pool[0] <= 0) return false;
        pool[0]--;
        return true;
    }

    /** 남은 부활 토큰 수 (없으면 0). */
    public int deathsRemaining(int slotId) {
        int[] pool = slotDeaths.get(slotId);
        return pool == null ? 0 : pool[0];
    }

    /** 최대 부활 토큰 수 (없으면 0). */
    public int deathsMax(int slotId) {
        int[] pool = slotDeaths.get(slotId);
        return pool == null ? 0 : pool[1];
    }

    /** 클리어/실패 종료 시 runId 기준으로 슬롯 + 참가자 모두 해제. */
    public void releaseByRunId(String runId) {
        Integer slotId = runToSlot.remove(runId);
        if (slotId == null) return;
        slotToRunId.remove(slotId);
        Set<UUID> toRemove = playerToSlot.entrySet().stream()
                .filter(e -> e.getValue().equals(slotId))
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
        toRemove.forEach(playerToSlot::remove);
        releaseSlot(slotId);
    }

    public void releaseSlot(int slotId) {
        slots.stream().filter(s -> s.id() == slotId).findFirst().ifPresent(BossRoomSlot::release);
        slotToBoss.remove(slotId);
        slotDeaths.remove(slotId);
    }

    /** slotId로 슬롯을 조회한다 (리스폰 좌표 등). */
    public Optional<BossRoomSlot> slotById(int slotId) {
        return slots.stream().filter(s -> s.id() == slotId).findFirst();
    }

    public long  freeCount()  { return slots.stream().filter(BossRoomSlot::isFree).count(); }
    public int   totalCount() { return slots.size(); }
    public List<BossRoomSlot> allSlots() { return slots; }

    // ── 보스 선택 / 클리어 추적 ──────────────────────────────────────

    public void             setPendingBoss(UUID uuid, String bossId) { pendingBoss.put(uuid, bossId); }
    public Optional<String> getPendingBoss(UUID uuid)                { return Optional.ofNullable(pendingBoss.get(uuid)); }
    public void             clearPendingBoss(UUID uuid)              { pendingBoss.remove(uuid); }

    public void    markCleared(UUID uuid, String bossId) { clearedBosses.computeIfAbsent(uuid, k -> ConcurrentHashMap.newKeySet()).add(bossId); }
    public boolean hasCleared(UUID uuid, String bossId)  { ensureClearsLoaded(uuid); Set<String> s = clearedBosses.get(uuid); return s != null && s.contains(bossId); }
}
