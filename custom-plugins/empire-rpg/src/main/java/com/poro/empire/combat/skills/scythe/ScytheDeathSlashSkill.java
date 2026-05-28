package com.poro.empire.combat.skills.scythe;

import com.poro.empire.combat.SkillContext;
import com.poro.empire.combat.hitbox.SkillHitboxHelper;
import com.poro.empire.combat.skills.BaseWeaponSkill;
import com.poro.empire.combat.weapon.WeaponType;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

public final class ScytheDeathSlashSkill extends BaseWeaponSkill {
    private static final Particle.DustOptions PURPLE = new Particle.DustOptions(Color.fromRGB(160, 0, 200), 1.2f);
    private static final Particle.DustOptions CRIMSON = new Particle.DustOptions(Color.fromRGB(180, 0, 60), 1.0f);

    public ScytheDeathSlashSkill() {
        super("scythe:death_slash", "사신베기", WeaponType.SCYTHE, 4000L);
    }

    @Override
    public boolean execute(Player player, SkillContext ctx) {
        double damage = scaledDamageWithStacks(ctx, player, 1.80, 0.05);
        SkillHitboxHelper.arc(player, 3.0, 150).forEach(t -> dealDamage(player, t, damage));

        // 보라 외호 + 검붉은 내호 + 바닐라 sweep 마크
        spawnParticleArc(player, Particle.DUST, PURPLE, 2.5, 150, 12);
        spawnParticleArc(player, Particle.DUST, CRIMSON, 1.5, 150,  8);
        spawnParticleArc(player, Particle.SWEEP_ATTACK, null, 2.0, 150, 5);
        playSound(player, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.0f, 0.75f);
        return true;
    }
}
