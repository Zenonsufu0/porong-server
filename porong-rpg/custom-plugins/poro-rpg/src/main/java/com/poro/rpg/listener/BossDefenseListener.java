package com.poro.rpg.listener;

import com.poro.rpg.boss.room.BossDamageTracker;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 인스턴스 보스 원샷 방지 클램프 (final_master_plan §9 / DL-091).
 * 추적 중인 보스 mob이 받는 단일 타격을 최대체력의 85%로 상한하고, 같은 타이밍(≈1틱) 후속 타격은 0으로 만든다
 * (다단 히트 스킬이 N×85%로 우회하는 것을 차단). 기여도 집계({@link BossInstanceDamageListener}, NORMAL)보다
 * 먼저 실행되도록 LOW 우선순위 — 집계가 클램프된 실제 피해를 반영하게 한다.
 *
 * <p>1차 시즌은 커스텀 보스 DEF 데이터(시드)가 없어 DEF/(DEF+200) 경감은 적용하지 않는다(보스 능력치는 MM 소관).
 * 본 리스너는 원샷 방지에만 집중한다.
 */
public final class BossDefenseListener implements Listener {

    private static final double ONESHOT_CAP_RATIO = 0.85;
    private static final long SAME_BURST_WINDOW_MS = 60L; // ≈1틱(50ms) 여유

    private final BossDamageTracker damageTracker;
    /** 보스 UUID → 마지막으로 상한이 걸린 시각(ms). 같은 타이밍 후속 0 판정용. */
    private final Map<UUID, Long> recentlyCapped = new ConcurrentHashMap<>();

    public BossDefenseListener(BossDamageTracker damageTracker) {
        this.damageTracker = Objects.requireNonNull(damageTracker, "damageTracker");
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        UUID victim = event.getEntity().getUniqueId();
        if (!damageTracker.isTracked(victim)) return;
        if (!(event.getEntity() instanceof LivingEntity living)) return;

        var maxAttr = living.getAttribute(Attribute.MAX_HEALTH);
        if (maxAttr == null) return;
        double cap = maxAttr.getValue() * ONESHOT_CAP_RATIO;
        if (cap <= 0) return;

        long now = System.currentTimeMillis();
        Long last = recentlyCapped.get(victim);
        if (last != null && now - last <= SAME_BURST_WINDOW_MS) {
            // 같은 타이밍 후속 타격 → 0 피해 (다단 히트 우회 차단)
            event.setDamage(0.0);
            return;
        }
        if (event.getFinalDamage() > cap) {
            event.setDamage(cap);
            recentlyCapped.put(victim, now);
        }
    }

    /** 보스 종료/제거 시 호출해 누수 방지(선택). */
    public void forget(UUID bossUuid) {
        if (bossUuid != null) recentlyCapped.remove(bossUuid);
    }
}
