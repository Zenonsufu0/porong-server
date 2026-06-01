package com.poro.rpg.combat.skills.scythe;

import com.poro.rpg.combat.SkillContext;
import com.poro.rpg.combat.hitbox.SkillHitboxHelper;
import com.poro.rpg.combat.skills.PluginWeaponSkill;
import com.poro.rpg.combat.weapon.WeaponType;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.List;

public final class ScytheShadowSpinSkill extends PluginWeaponSkill {
    private static final Particle.DustOptions VIOLET = new Particle.DustOptions(Color.fromRGB(100, 0, 180), 1.0f);

    public ScytheShadowSpinSkill(Plugin plugin) {
        super(plugin, "scythe:shadow_spin", "월영회전", WeaponType.SCYTHE, 5000L);
    }

    @Override
    public boolean execute(Player player, SkillContext ctx) {
        // 시선 방향 대시. (getVelocity는 서버측 플레이어 입력을 반영하지 않아 항상 우측으로 새던 버그)
        dashForward(player, 2.5);
        double damage = scaledDamage(ctx, player, 0.60);
        boolean hitAny = false;
        for (int i = 0; i < 4; i++) {
            List<LivingEntity> targets = SkillHitboxHelper.burst(player, 3.0);
            if (!targets.isEmpty()) {
                targets.forEach(t -> dealDamage(ctx, player, t, damage));
                hitAny = true;
            }
        }
        // 사용 시점에 2초 윈도우 기록 — 사신베기 LMB 명중 시 1스택 충전
        ctx.getResourceTracker().recordShadowSpin(player.getUniqueId());

        // 플레이어 주변 보라 ring + 바닐라 sweep 마크
        spawnParticleCircle(player, Particle.DUST, VIOLET, 2.0, 16);
        spawnParticleCircle(player, Particle.SWEEP_ATTACK, null, 1.5, 6);
        playSound(player, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.0f, 1.4f);
        return true;
    }
}
