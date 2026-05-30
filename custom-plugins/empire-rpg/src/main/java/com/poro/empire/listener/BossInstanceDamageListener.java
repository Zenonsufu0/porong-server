package com.poro.empire.listener;

import com.poro.empire.boss.room.BossDamageTracker;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import java.util.Objects;
import java.util.UUID;

/**
 * 인스턴스 보스(보스룸) 데미지 기여 추적 (INBOX-004 #5 / DL-084).
 * 추적 대상으로 등록된 보스 mob이 데미지를 받으면 BossDamageTracker에 가해자별 누적.
 * 필드보스는 FieldDropListener가 별도 처리(태그 기반) — 인스턴스 보스 mob UUID와 겹치지 않음.
 */
public final class BossInstanceDamageListener implements Listener {
    private final BossDamageTracker damageTracker;

    public BossInstanceDamageListener(BossDamageTracker damageTracker) {
        this.damageTracker = Objects.requireNonNull(damageTracker, "damageTracker");
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

    private Player resolvePlayer(Entity damager) {
        if (damager instanceof Player p) return p;
        if (damager instanceof Projectile proj && proj.getShooter() instanceof Player p) return p;
        return null;
    }
}
