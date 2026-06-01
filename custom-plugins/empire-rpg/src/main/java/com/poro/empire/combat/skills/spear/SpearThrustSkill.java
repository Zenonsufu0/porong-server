package com.poro.empire.combat.skills.spear;

import com.poro.empire.combat.SkillContext;
import com.poro.empire.combat.hitbox.SkillHitboxHelper;
import com.poro.empire.combat.skills.BaseWeaponSkill;
import com.poro.empire.combat.weapon.WeaponType;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

public final class SpearThrustSkill extends BaseWeaponSkill {
    // pt:spear_pierce_line — 청록 관통 찌르기
    private static final Particle.DustOptions TEAL = new Particle.DustOptions(Color.fromRGB(90, 220, 210), 1.0f);

    public SpearThrustSkill() {
        super("spear:thrust", "관통찌르기", WeaponType.SPEAR, 3000L);
    }

    @Override
    public boolean execute(Player player, SkillContext ctx) {
        double damage = scaledDamageWithStacks(ctx, player, 1.60, 0.05);
        var targets = SkillHitboxHelper.line(player, 5.0, 0.5);
        targets.forEach(t -> dealDamage(ctx, player, t, damage));
        if (!targets.isEmpty()) gainStack(ctx, player, 3);   // 명중 시에만 충전 (정본 §4)

        // 날카로운 청록 직선 + 관통 스파크
        spawnParticleLine(player, Particle.DUST, TEAL, 5.0, 24);
        spawnParticleLine(player, Particle.CRIT, null, 5.0, 14);
        playSound(player, Sound.ENTITY_PLAYER_ATTACK_STRONG, 0.9f, 1.3f);
        return true;
    }
}
