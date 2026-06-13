package kr.zenon.rpg.listener;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.potion.PotionEffectType;

import java.util.Set;

/**
 * 바닐라 몹이 거는 방해성 디버프를 플레이어에게서 차단 (DL-129 추가#23).
 * 포로 서버는 자체 전투/상태 시스템을 쓰므로, 바닐라 보스(위더 스켈레톤=위더, 가디언=채굴피로,
 * 워든=어둠 등)가 주는 부정 효과는 노이즈라 제거한다. 플레이어가 직접 마실 일이 없는 효과들만 막으므로
 * 출처(CAUSE) 구분 없이 전역 차단해도 안전하다.
 */
public final class VanillaDebuffBlockListener implements Listener {

    private static final Set<PotionEffectType> BLOCKED = Set.of(
            PotionEffectType.WITHER,          // 위더 (위더 스켈레톤 타격)
            PotionEffectType.MINING_FATIGUE,  // 채굴 피로 (가디언/엘더 가디언)
            PotionEffectType.DARKNESS,        // 어둠 (워든) — 보스전 시야 방해
            PotionEffectType.BLINDNESS        // 실명 — 시야 차단 (일부 보스 <25% 패턴)
    );

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPotionEffect(EntityPotionEffectEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        EntityPotionEffectEvent.Action action = event.getAction();
        if (action != EntityPotionEffectEvent.Action.ADDED
                && action != EntityPotionEffectEvent.Action.CHANGED) return;
        PotionEffectType type = event.getModifiedType();
        if (type != null && BLOCKED.contains(type)) {
            event.setCancelled(true);
        }
    }
}
