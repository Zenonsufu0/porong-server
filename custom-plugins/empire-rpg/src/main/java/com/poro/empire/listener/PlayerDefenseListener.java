package com.poro.empire.listener;

import com.poro.empire.combat.SkillContext;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

/**
 * 몹 → 플레이어 피격 시 방어구 DEF 경감 적용 (DL-113 1단계).
 *
 * <p>전투가 스킬 기반(플레이어 좌클릭=스킬, 데미지는 CombatFormulaResolver)이라 무기 ATK는
 * 공격 측에서 처리되지만, 몹이 플레이어를 때리는 경로는 바닐라 데미지라 방어구 DEF가 적용되지 않았다.
 * 이 리스너가 받는 쪽(플레이어 방어)에 {@code DEF/(DEF+200)} 감쇠를 적용한다.</p>
 *
 * <p>범위(1단계): 몹/몹 투사체만. PvP(플레이어 가해)·환경 피해는 제외. 베이스 DEF만 반영
 * (강화/잠재/인내 가산은 2~3단계). 분모 200은 plyaer→boss 역산 공유 상수라 건드리지 않는다.</p>
 */
public final class PlayerDefenseListener implements Listener {

    private static final double DEF_DENOMINATOR = 200.0d;

    private final SkillContext skillContext;

    public PlayerDefenseListener(SkillContext skillContext) {
        this.skillContext = skillContext;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPlayerDamaged(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) return;
        if (isPlayerSource(event.getDamager())) return; // PvP 제외 — 1단계는 몹→플레이어만

        double def = skillContext.defense(victim);
        if (def <= 0.0d) return;
        double mitigation = def / (def + DEF_DENOMINATOR);
        event.setDamage(event.getDamage() * (1.0d - mitigation));
    }

    /** 가해자가 플레이어(직접 또는 투사체 발사자)면 true — PvP로 간주해 제외. */
    private boolean isPlayerSource(Entity damager) {
        if (damager instanceof Player) return true;
        if (damager instanceof Projectile proj) {
            ProjectileSource shooter = proj.getShooter();
            return shooter instanceof Player;
        }
        return false;
    }
}
