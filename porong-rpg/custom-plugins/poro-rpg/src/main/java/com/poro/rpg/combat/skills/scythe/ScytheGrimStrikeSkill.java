package com.poro.rpg.combat.skills.scythe;

import com.poro.rpg.combat.SkillContext;
import com.poro.rpg.combat.hitbox.SkillHitboxHelper;
import com.poro.rpg.combat.skills.BaseWeaponSkill;
import com.poro.rpg.combat.weapon.WeaponType;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

public final class ScytheGrimStrikeSkill extends BaseWeaponSkill {
    // pt:scythe_dark_strike — 음산한 보라/검붉은 그믐 부채꼴
    private static final Particle.DustOptions PURPLE = new Particle.DustOptions(Color.fromRGB(160, 0, 200), 1.2f);
    private static final Particle.DustOptions CRIMSON = new Particle.DustOptions(Color.fromRGB(180, 0, 60), 1.0f);

    public ScytheGrimStrikeSkill() {
        super("scythe:grim_strike", "그믐참", WeaponType.SCYTHE, 10000L);
    }

    @Override
    public boolean execute(Player player, SkillContext ctx) {
        int stacks = getStacks(ctx, player);
        double damage = scaledDamageWithStacks(ctx, player, 2.40, 0.08);
        double lifeStealAmount = damage * (0.06 * stacks);
        SkillHitboxHelper.cone(player, 4.0, 60).forEach(t -> {
            dealDamage(ctx, player, t, damage);
            if (lifeStealAmount > 0) lifesteal(ctx, player, lifeStealAmount);
        });

        // 좁은 부채꼴(cone 60) 보라+검붉은 + 마녀 입자 + 흡혈 음
        spawnParticleArc(player, Particle.DUST, PURPLE, 3.5, 60, 12);
        spawnParticleArc(player, Particle.DUST, CRIMSON, 2.0, 60, 8);
        spawnParticleArc(player, Particle.WITCH, null, 3.0, 60, 8);
        playSound(player, Sound.ENTITY_WITHER_SHOOT, 0.6f, 1.3f);
        return true;
    }
}
