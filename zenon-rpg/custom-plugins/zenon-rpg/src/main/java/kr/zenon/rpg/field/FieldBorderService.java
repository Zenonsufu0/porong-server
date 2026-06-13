package kr.zenon.rpg.field;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.WorldBorder;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 필드 진입 시 개인 WorldBorder(빨간 경계)를 띄운다 (INBOX-012).
 *
 * <p>플레이어가 필드 영역(중심 ±{@value #BOUND_RADIUS} = 300×300, {@code FieldSpawnService}의
 * 스폰 존과 일치) 안에 들어오면 그 필드 중심을 기준으로 한 개인 WorldBorder를 적용한다. 필드 밖으로
 * 나가면 해제(월드 기본 경계로 복귀). per-player border라 같은 월드에 있어도 각자 자기 필드 경계만 본다.</p>
 *
 * <p>데미지 0 — 이동 통제·시각 경계만. 필드 이탈은 GUI 이동을 통해 이뤄지는 설계(map_design)와 일치.</p>
 */
public final class FieldBorderService {

    private static final double BOUND_RADIUS = 150.0;   // FieldSpawnService와 동일 (300×300)
    private static final double BORDER_SIZE  = BOUND_RADIUS * 2.0;
    private static final int    WARNING_DIST = 8;       // 경계 근접 시 빨간 화면 표시 거리
    private static final long   PERIOD_TICKS = 10L;     // 0.5초 폴링

    private record FieldArea(String world, double cx, double cz) {
        String key() { return world + ":" + cx + ":" + cz; }
        boolean contains(Location loc) {
            return loc.getWorld() != null
                    && loc.getWorld().getName().equals(world)
                    && Math.abs(loc.getX() - cx) <= BOUND_RADIUS
                    && Math.abs(loc.getZ() - cz) <= BOUND_RADIUS;
        }
    }

    private final Plugin plugin;
    private final List<FieldArea> fields = new ArrayList<>();
    private final Map<UUID, String> applied = new HashMap<>(); // player → 적용된 필드 key (없으면 미적용)

    public FieldBorderService(Plugin plugin) {
        this.plugin = plugin;
    }

    public void start() {
        loadFields();
        if (fields.isEmpty()) {
            plugin.getLogger().info("[FieldBorder] config fields 없음 — 필드 경계 비활성.");
            return;
        }
        Bukkit.getScheduler().runTaskTimer(plugin, this::tick, PERIOD_TICKS, PERIOD_TICKS);
        plugin.getLogger().info("[FieldBorder] 필드 진입 경계 활성 — 필드 " + fields.size()
                + "개, 크기 " + (int) BORDER_SIZE + "×" + (int) BORDER_SIZE + ".");
    }

    private void loadFields() {
        ConfigurationSection sec = plugin.getConfig().getConfigurationSection("fields");
        if (sec == null) return;
        for (String id : sec.getKeys(false)) {
            ConfigurationSection fs = sec.getConfigurationSection(id);
            if (fs == null) continue;
            String world = fs.getString("world", "world");
            fields.add(new FieldArea(world, fs.getDouble("x"), fs.getDouble("z")));
        }
    }

    private void tick() {
        Set<UUID> online = new HashSet<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            UUID uuid = player.getUniqueId();
            online.add(uuid);

            FieldArea area = fieldAt(player.getLocation());
            String want = area == null ? null : area.key();
            String have = applied.get(uuid);
            if (java.util.Objects.equals(want, have)) continue; // 변화 없음 — 패킷 재전송 방지

            if (area == null) {
                player.setWorldBorder(null);   // 월드 기본 경계로 복귀
                applied.remove(uuid);
            } else {
                player.setWorldBorder(buildBorder(area));
                applied.put(uuid, want);
            }
        }
        applied.keySet().retainAll(online); // 오프라인 플레이어 정리
    }

    private WorldBorder buildBorder(FieldArea area) {
        WorldBorder border = Bukkit.createWorldBorder();
        border.setCenter(area.cx(), area.cz());
        border.setSize(BORDER_SIZE);
        border.setWarningDistance(WARNING_DIST);
        border.setDamageAmount(0.0);   // 경계 밖 데미지 없음 — 이동 통제·시각만
        border.setDamageBuffer(0.0);
        return border;
    }

    private FieldArea fieldAt(Location loc) {
        for (FieldArea f : fields) {
            if (f.contains(loc)) return f;
        }
        return null;
    }
}
