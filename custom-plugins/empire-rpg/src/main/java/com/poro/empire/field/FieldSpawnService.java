package com.poro.empire.field;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Predicate;

/**
 * 동적 필드 몹 스폰 (INBOX-006 / DL-NNN) — 코어.
 * 주기적으로 필드(300×300, config 중심 ±150) 안의 각 플레이어 주변에 MM 몹을 배치한다.
 * 2단 캡(플레이어당 / 필드 전체) + 소유자 추적 + 오프라인/사망 정리.
 *
 * <p>일반/정예 모드는 {@code eliteMode} predicate로 외부 주입(2차에서 명령어·GUI 연결).
 * 랜덤 진입·고급 디스폰은 후속 단계.
 */
public final class FieldSpawnService {

    // 튜닝 상수 (INBOX-006 시작값)
    private static final long   INTERVAL_TICKS = 300L; // 15초
    private static final int    BATCH          = 12;   // 웨이브당 스폰 수(일반)
    private static final int    PLAYER_CAP     = 40;   // 플레이어당 standing 상한
    private static final int    FIELD_CAP      = 250;  // 필드 전체 상한
    private static final double BOUND_RADIUS   = 150.0; // 300×300 → ±150
    private static final double SPAWN_MIN      = 20.0;
    private static final double SPAWN_MAX      = 30.0;

    /** config 필드 id → 필드 인덱스(1~5). */
    private static final Map<String, Integer> FIELD_INDEX = Map.of(
            "plain", 1, "mine", 2, "sewer", 3, "outpost", 4, "ruins", 5);

    /** 필드 인덱스 → 일반 몹 MM 내부명(랜덤 선택). */
    private static final Map<Integer, String[]> NORMAL_MOBS = Map.of(
            1, new String[]{"Plains_Soldier", "Plains_Wildling"},
            2, new String[]{"Mine_Crawler", "Mine_Husk"},
            3, new String[]{"Waterway_Drowned", "Waterway_Guardian"},
            4, new String[]{"Outpost_Pillager", "Outpost_Vindicator"},
            5, new String[]{"Wall_Enderman", "Wall_Shade"});

    /** 필드 인덱스 → 정예 몹 MM 내부명. */
    private static final Map<Integer, String> ELITE_MOB = Map.of(
            1, "Plains_StalkerElite", 2, "Mine_WatcherElite", 3, "Waterway_LurkerElite",
            4, "Outpost_CaptainElite", 5, "Wall_SentinelElite");

    private final Plugin plugin;
    private final BiFunction<String, Location, UUID> mythicSpawner; // (mobId, loc) → 스폰된 UUID (실패 null)
    private final Predicate<UUID> eliteMode;

    private final List<FieldDef> fields = new ArrayList<>();
    /** 플레이어 UUID → 그가 소유한(스폰시킨) 살아있는 몹 UUID 집합. */
    private final Map<UUID, Set<UUID>> ownedMobs = new HashMap<>();
    /** 몹 UUID → 필드 인덱스 (필드 전체 캡 집계용). */
    private final Map<UUID, Integer> mobField = new HashMap<>();
    private final Random rng = new Random();

    private record FieldDef(String id, int index, World world, double cx, double cz) {}

    public FieldSpawnService(Plugin plugin,
                             BiFunction<String, Location, UUID> mythicSpawner,
                             Predicate<UUID> eliteMode) {
        this.plugin        = Objects.requireNonNull(plugin, "plugin");
        this.mythicSpawner = mythicSpawner;
        this.eliteMode     = eliteMode != null ? eliteMode : (u -> false);
    }

    public void start() {
        if (mythicSpawner == null) {
            plugin.getLogger().warning("[FieldSpawn] MythicMobs 미가용 — 동적 필드 스폰 비활성.");
            return;
        }
        loadFields();
        if (fields.isEmpty()) {
            plugin.getLogger().warning("[FieldSpawn] config fields 정의 없음 — 동적 필드 스폰 비활성.");
            return;
        }
        Bukkit.getScheduler().runTaskTimer(plugin, this::tick, INTERVAL_TICKS, INTERVAL_TICKS);
        plugin.getLogger().info("[FieldSpawn] 동적 필드 스폰 활성 — " + fields.size()
                + "개 필드, 주기 " + (INTERVAL_TICKS / 20) + "s, 플레이어캡 " + PLAYER_CAP + "/필드캡 " + FIELD_CAP + ".");
    }

    private void loadFields() {
        ConfigurationSection sec = plugin.getConfig().getConfigurationSection("fields");
        if (sec == null) return;
        for (String id : sec.getKeys(false)) {
            Integer idx = FIELD_INDEX.get(id);
            if (idx == null) continue;
            ConfigurationSection fs = sec.getConfigurationSection(id);
            if (fs == null) continue;
            World w = Bukkit.getWorld(fs.getString("world", "world"));
            if (w == null) continue;
            fields.add(new FieldDef(id, idx, w, fs.getDouble("x"), fs.getDouble("z")));
        }
    }

    private void tick() {
        pruneDeadAndOffline();

        // 필드 전체 몹 수 히스토그램
        Map<Integer, Integer> fieldCount = new HashMap<>();
        for (Integer idx : mobField.values()) fieldCount.merge(idx, 1, Integer::sum);

        for (Player player : Bukkit.getOnlinePlayers()) {
            int field = fieldAt(player.getLocation());
            if (field == 0) continue;

            Set<UUID> owned = ownedMobs.computeIfAbsent(player.getUniqueId(), k -> new HashSet<>());
            int playerCount = owned.size();
            if (playerCount >= PLAYER_CAP) continue;

            int fc = fieldCount.getOrDefault(field, 0);
            if (fc >= FIELD_CAP) continue;

            boolean elite = eliteMode.test(player.getUniqueId());
            int batch = elite ? Math.max(1, BATCH / 3) : BATCH; // 정예는 수 적게
            int toSpawn = Math.min(batch, Math.min(PLAYER_CAP - playerCount, FIELD_CAP - fc));

            for (int i = 0; i < toSpawn; i++) {
                String mobId = pickMob(field, elite);
                if (mobId == null) break;
                Location loc = randomGroundAround(player);
                if (loc == null) continue;
                UUID mob = mythicSpawner.apply(mobId, loc);
                if (mob == null) continue;
                owned.add(mob);
                mobField.put(mob, field);
                fieldCount.merge(field, 1, Integer::sum);
            }
        }
    }

    /** 사망/제거된 몹 untrack + 오프라인 소유자 몹 디스폰. */
    private void pruneDeadAndOffline() {
        for (var it = ownedMobs.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<UUID, Set<UUID>> e = it.next();
            boolean ownerOffline = Bukkit.getPlayer(e.getKey()) == null;
            Set<UUID> mobs = e.getValue();
            for (var mit = mobs.iterator(); mit.hasNext(); ) {
                UUID mobUuid = mit.next();
                Entity ent = Bukkit.getEntity(mobUuid);
                if (ent == null || ent.isDead()) {
                    mit.remove();
                    mobField.remove(mobUuid);
                } else if (ownerOffline) {
                    ent.remove(); // 소유자 로그아웃 → 몹 정리
                    mit.remove();
                    mobField.remove(mobUuid);
                }
            }
            if (ownerOffline && mobs.isEmpty()) it.remove();
        }
    }

    /** 위치가 속한 필드 인덱스(없으면 0). */
    private int fieldAt(Location loc) {
        World w = loc.getWorld();
        if (w == null) return 0;
        for (FieldDef f : fields) {
            if (!f.world.equals(w)) continue;
            if (Math.abs(loc.getX() - f.cx) <= BOUND_RADIUS && Math.abs(loc.getZ() - f.cz) <= BOUND_RADIUS) {
                return f.index;
            }
        }
        return 0;
    }

    private String pickMob(int field, boolean elite) {
        if (elite) return ELITE_MOB.get(field);
        String[] pool = NORMAL_MOBS.get(field);
        return (pool == null || pool.length == 0) ? null : pool[rng.nextInt(pool.length)];
    }

    /** 플레이어 주변 20~30블록 지면 위치. */
    private Location randomGroundAround(Player player) {
        Location base = player.getLocation();
        World w = base.getWorld();
        if (w == null) return null;
        double angle = rng.nextDouble() * Math.PI * 2;
        double dist = SPAWN_MIN + rng.nextDouble() * (SPAWN_MAX - SPAWN_MIN);
        int x = (int) Math.round(base.getX() + Math.cos(angle) * dist);
        int z = (int) Math.round(base.getZ() + Math.sin(angle) * dist);
        int y = w.getHighestBlockYAt(x, z) + 1;
        return new Location(w, x + 0.5, y, z + 0.5);
    }
}
