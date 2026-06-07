package com.poro.rpg.listener;

import com.poro.rpg.boss.engine.BossRunService;
import com.poro.rpg.boss.room.BossDamageTracker;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;

import java.util.Objects;
import java.util.UUID;

/**
 * 인스턴스 보스(보스룸) 데미지 기여 추적 + 처치→클리어 종료 (INBOX-004 #5 / DL-084, DL-091).
 * 추적 대상으로 등록된 보스 mob이 데미지를 받으면 BossDamageTracker에 가해자별 누적하고,
 * 그 보스가 죽으면 runId를 찾아 {@code endRun(true)}로 클리어 처리한다(보상·markCleared·damage_share는 endRun 훅 체인).
 * 필드보스는 FieldDropListener가 별도 처리(태그 기반) — 인스턴스 보스 mob UUID와 겹치지 않음.
 */
public final class BossInstanceDamageListener implements Listener {
    private final BossDamageTracker damageTracker;
    private final BossRunService runService;

    public BossInstanceDamageListener(BossDamageTracker damageTracker, BossRunService runService) {
        this.damageTracker = Objects.requireNonNull(damageTracker, "damageTracker");
        this.runService    = Objects.requireNonNull(runService, "runService");
    }

    @EventHandler(ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        UUID victim = event.getEntity().getUniqueId();
        if (!damageTracker.isTracked(victim)) return;
        double dmg = event.getFinalDamage();
        if (dmg <= 0) return;
        Player player = resolvePlayer(event.getDamager());
        if (player == null) return;
        damageTracker.recordDamage(victim, player.getUniqueId(), dmg);
    }

    /** 추적 중인 보스 mob 사망 → 해당 런 클리어 종료. endRun이 비활성 런이면 무시(멱등). */
    @EventHandler
    public void onBossDeath(EntityDeathEvent event) {
        String runId = damageTracker.runIdForMob(event.getEntity().getUniqueId());
        if (runId == null) return;
        runService.endRun(runId, true, "");
    }

    private Player resolvePlayer(Entity damager) {
        if (damager instanceof Player p) return p;
        if (damager instanceof Projectile proj && proj.getShooter() instanceof Player p) return p;
        return null;
    }
}
