package com.poro.empire.combat.skills.scythe;

import com.poro.empire.combat.SkillContext;
import com.poro.empire.combat.hitbox.SkillHitboxHelper;
import com.poro.empire.combat.skills.BaseWeaponSkill;
import com.poro.empire.combat.weapon.WeaponType;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;

public final class ScytheExecutionSkill extends BaseWeaponSkill {
    // pt:scythe_execute_slash — 처형, 보라 직선 + 영혼불
    private static final Particle.DustOptions PURPLE = new Particle.DustOptions(Color.fromRGB(160, 0, 200), 1.3f);

    public ScytheExecutionSkill() {
        super("scythe:execution", "처형낫", WeaponType.SCYTHE, 16000L);
    }

    @Override
    public boolean execute(Player player, SkillContext ctx) {
        consumeStacks(ctx, player);
        SkillHitboxHelper.line(player, 4.0, 1.0).forEach(t -> {
            var maxHpAttr = t.getAttribute(Attribute.MAX_HEALTH);
            double hpRatio = (maxHpAttr != null && maxHpAttr.getValue() > 0)
                    ? t.getHealth() / maxHpAttr.getValue() : 1.0;
            double coeff = hpRatio < 0.30 ? 4.80 : 2.80;
            dealDamage(player, t, scaledDamage(ctx, player, coeff));
        });

        // 처형 직선 + 영혼불 + sweep + 음산한 처형음
        spawnParticleLine(player, Particle.DUST, PURPLE, 4.0, 24);
        spawnParticleLine(player, Particle.SOUL_FIRE_FLAME, null, 4.0, 14);
        spawnParticleArc(player, Particle.SWEEP_ATTACK, null, 3.0, 120, 4);
        playSound(player, Sound.ENTITY_WITHER_HURT, 0.8f, 0.7f);
        playSound(player, Sound.ENTITY_PLAYER_ATTACK_CRIT, 1.0f, 0.6f);
        return true;
    }
}
