package com.poro.rpg.combat.skills.sword;

import com.poro.rpg.combat.SkillContext;
import com.poro.rpg.combat.hitbox.SkillHitboxHelper;
import com.poro.rpg.combat.skills.PluginWeaponSkill;
import com.poro.rpg.combat.weapon.WeaponType;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public final class SwordTripleStrikeSkill extends PluginWeaponSkill {
    // pt:sword_combo_slash — 강철 3연 베기
    private static final Particle.DustOptions STEEL = new Particle.DustOptions(Color.fromRGB(200, 220, 235), 1.0f);

    public SwordTripleStrikeSkill(Plugin plugin) {
        super(plugin, "sword:triple_strike", "연속참", WeaponType.SWORD, 6000L);
    }

    @Override
    public boolean execute(Player player, SkillContext ctx) {
        double damage = scaledDamage(ctx, player, 0.70);
        for (int i = 0; i < 3; i++) {
            SkillHitboxHelper.arc(player, 2.5, 120).forEach(t -> dealDamage(ctx, player, t, damage));
        }

        // 3중 호 + sweep 마크 + 타격 스파크(CRIT)
        spawnParticleArc(player, Particle.DUST, STEEL, 2.5, 120, 12);
        spawnParticleArc(player, Particle.DUST, STEEL, 1.8, 120, 9);
        spawnParticleArc(player, Particle.DUST, STEEL, 1.1, 120, 6);
        spawnParticleArc(player, Particle.SWEEP_ATTACK, null, 2.0, 120, 3);
        spawnParticleArc(player, Particle.CRIT, null, 2.4, 120, 10);
        playSound(player, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.0f, 1.1f);
        return true;
    }
}
