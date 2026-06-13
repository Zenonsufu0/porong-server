package kr.zenon.rpg.combat;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * 데미지/회복 숫자를 TextDisplay로 대상 머리 위에 띄우는 서비스.
 *
 * <p>설계:
 * <ul>
 *   <li>기본은 자기 데미지만 표시 ({@code visibleByDefault=false} + 공격자에게만 showEntity).
 *       파티원 데미지 표시는 옵션(기본 OFF) — {@link #setPartyVisible}.</li>
 *   <li>다단히트는 즉시 띄우지 않고 0.2초(4틱) 누적 후 합산 1개로 표시.
 *       도트는 1초(20틱) 단위 합산.</li>
 *   <li>누적 키 = viewer_uuid + target_uuid + type.</li>
 *   <li>TextDisplay는 12~18틱 후 자동 제거. 위치는 머리 위 + 랜덤 x/z 오프셋.</li>
 * </ul>
 *
 * <p>스킬 코드는 {@link #addDamage}/{@link #addHeal}만 호출한다.
 * 모든 메서드는 메인 스레드에서 호출된다(스킬 실행·이벤트 모두 메인 스레드).
 */
public final class DamageNumberService {

    public enum Type { NORMAL_DAMAGE, SKILL_DAMAGE, CRITICAL_DAMAGE, HEAL, LIFESTEAL, DOT_DAMAGE }

    private static final int HIT_WINDOW = 4;    // 0.2s — 다단히트 누적
    private static final int DOT_WINDOW = 20;   // 1s   — 도트 누적
    private static final int LIFE_MIN = 12, LIFE_MAX = 18;

    // 타입별 색상
    private static final TextColor C_NORMAL = NamedTextColor.WHITE;
    private static final TextColor C_SKILL  = TextColor.color(0xF5F5A0); // pale yellow
    private static final TextColor C_CRIT   = NamedTextColor.GOLD;
    private static final TextColor C_HEAL   = NamedTextColor.GREEN;
    private static final TextColor C_LIFE   = TextColor.color(0xFF6B8A); // crimson/pink
    private static final TextColor C_DOT    = TextColor.color(0x9B6BD6); // purple

    private final JavaPlugin plugin;
    private final Map<String, Accum> pending = new HashMap<>();       // 메인 스레드 전용
    private final Set<UUID> partyVisible = ConcurrentHashMap.newKeySet();
    private Function<UUID, Collection<UUID>> partyResolver = u -> List.of();
    private final Random rng = new Random();

    public DamageNumberService(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    /** 파티 멤버 조회자 주입 — attacker UUID → 같은 파티 멤버 UUID들. 미설정 시 자기만. */
    public void setPartyResolver(Function<UUID, Collection<UUID>> resolver) {
        if (resolver != null) this.partyResolver = resolver;
    }

    public boolean isPartyVisible(UUID uuid) { return partyVisible.contains(uuid); }

    public void setPartyVisible(UUID uuid, boolean on) {
        if (on) partyVisible.add(uuid); else partyVisible.remove(uuid);
    }

    // ─── 공개 API ────────────────────────────────────────────────────────

    /** 대상에게 가한 피해 숫자 누적. type=NORMAL/SKILL/CRITICAL/DOT. */
    public void addDamage(Player attacker, Entity target, double amount, Type type) {
        if (attacker == null || target == null || amount <= 0) return;
        enqueue(attacker.getUniqueId(), target, amount, type);
    }

    /** 플레이어 머리 위 회복/흡혈 숫자 누적. type=HEAL/LIFESTEAL. */
    public void addHeal(Player player, double amount, Type type) {
        if (player == null || amount <= 0) return;
        enqueue(player.getUniqueId(), player, amount, type);
    }

    // ─── 누적 ────────────────────────────────────────────────────────────

    private void enqueue(UUID viewer, Entity target, double amount, Type type) {
        String key = viewer + "|" + target.getUniqueId() + "|" + type;
        Accum a = pending.get(key);
        if (a == null) {
            a = new Accum(viewer, target.getUniqueId(), type);
            pending.put(key, a);
            int window = (type == Type.DOT_DAMAGE) ? DOT_WINDOW : HIT_WINDOW;
            Bukkit.getScheduler().runTaskLater(plugin, () -> flush(key), window);
        }
        a.sum += amount;
        a.loc = headLocation(target); // 최신 위치 갱신(대상 이동 추종)
    }

    private void flush(String key) {
        Accum a = pending.remove(key);
        if (a != null) spawnTextDisplay(a);
    }

    /** 대기 중인 누적을 즉시 모두 표시(플러그인 종료 등). */
    public void flushQueue() {
        for (String key : new ArrayList<>(pending.keySet())) flush(key);
    }

    // ─── 표시 ────────────────────────────────────────────────────────────

    private void spawnTextDisplay(Accum a) {
        Location base = a.loc;
        Entity ent = Bukkit.getEntity(a.target);
        if (ent != null && !ent.isDead()) base = headLocation(ent); // 살아있으면 현재 위치
        if (base == null || base.getWorld() == null) return;

        // 겹침 방지 랜덤 오프셋
        Location loc = base.clone().add((rng.nextDouble() - 0.5) * 0.8, 0, (rng.nextDouble() - 0.5) * 0.8);
        Component text = format(a.type, Math.round(a.sum));

        // 뷰어: 공격자 + 파티 표시 옵션 ON인 파티원
        Set<UUID> viewers = new HashSet<>();
        viewers.add(a.viewer);
        for (UUID m : partyResolver.apply(a.viewer)) {
            if (partyVisible.contains(m)) viewers.add(m);
        }

        TextDisplay display = loc.getWorld().spawn(loc, TextDisplay.class, d -> {
            d.text(text);
            d.setBillboard(Display.Billboard.CENTER);
            d.setDefaultBackground(false);
            d.setBackgroundColor(Color.fromARGB(0, 0, 0, 0)); // 배경 박스 제거
            d.setShadowed(true);
            d.setVisibleByDefault(false);                     // 기본 비표시 → 뷰어에게만
            d.setPersistent(false);
        });
        for (UUID v : viewers) {
            Player p = Bukkit.getPlayer(v);
            if (p != null) p.showEntity(plugin, display);
        }

        int life = LIFE_MIN + rng.nextInt(LIFE_MAX - LIFE_MIN + 1);
        Bukkit.getScheduler().runTaskLater(plugin,
                () -> { if (display.isValid()) display.remove(); }, life);
    }

    private Component format(Type type, long n) {
        return switch (type) {
            case NORMAL_DAMAGE   -> Component.text(n).color(C_NORMAL);
            case SKILL_DAMAGE    -> Component.text(n).color(C_SKILL);
            case CRITICAL_DAMAGE -> Component.text("✦ " + n + "!").color(C_CRIT).decorate(TextDecoration.BOLD);
            case HEAL            -> Component.text("+" + n).color(C_HEAL);
            case LIFESTEAL       -> Component.text("+" + n).color(C_LIFE);
            case DOT_DAMAGE      -> Component.text(n).color(C_DOT);
        };
    }

    private static Location headLocation(Entity e) {
        if (e == null) return null;
        double h = (e instanceof LivingEntity le) ? le.getEyeHeight() : e.getHeight();
        return e.getLocation().clone().add(0, h + 0.4, 0);
    }

    private static final class Accum {
        double sum;
        Location loc;
        final UUID viewer;
        final UUID target;
        final Type type;

        Accum(UUID viewer, UUID target, Type type) {
            this.viewer = viewer;
            this.target = target;
            this.type = type;
        }
    }
}
