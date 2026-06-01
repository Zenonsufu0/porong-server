package com.poro.rpg.combat.skills.spear;

import com.poro.rpg.combat.SkillContext;
import com.poro.rpg.combat.hitbox.SkillHitboxHelper;
import com.poro.rpg.combat.skills.BaseWeaponSkill;
import com.poro.rpg.combat.weapon.WeaponType;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

public final class SpearChargeSkill extends BaseWeaponSkill {
    // pt:spear_dash_trail — 돌파 궤적 (청록 직선 + 구름 잔류)
    private static final Particle.DustOptions TEAL = new Particle.DustOptions(Color.fromRGB(90, 220, 210), 1.1f);

    public SpearChargeSkill() {
        super("spear:charge", "돌파창", WeaponType.SPEAR, 9000L);
    }

    @Override
    public boolean execute(Player player, SkillContext ctx) {
        dashForward(player, 5.0);
        double damage = scaledDamage(ctx, player, 2.60);
        SkillHitboxHelper.line(player, 5.0, 1.0).forEach(t -> dealDamage(ctx, player, t, damage));

        // 돌파 궤적 직선 + 구름 잔류 + 추진음
        spawnParticleLine(player, Particle.DUST, TEAL, 5.0, 26);
        spawnParticleLine(player, Particle.CLOUD, null, 5.0, 16);
        playSound(player, Sound.ENTITY_PLAYER_ATTACK_KNOCKBACK, 1.0f, 0.9f);
        return true;
    }
}
