package com.poro.empire.combat.skills.sword;

import com.poro.empire.combat.SkillContext;
import com.poro.empire.combat.hitbox.SkillHitboxHelper;
import com.poro.empire.combat.skills.PluginWeaponSkill;
import com.poro.empire.combat.weapon.WeaponType;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public final class SwordGuardCounterSkill extends PluginWeaponSkill {
    // pt:sword_guard_counter — 청색 방어 고리 + 반격
    private static final Particle.DustOptions GUARD = new Particle.DustOptions(Color.fromRGB(120, 200, 255), 1.2f);

    public SwordGuardCounterSkill(Plugin plugin) {
        super(plugin, "sword:guard_counter", "수호반격", WeaponType.SWORD, 10000L);
    }

    @Override
    public boolean execute(Player player, SkillContext ctx) {
        // Guard: brief resistance buff
        player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 40, 2, false, false, true));
        // Counter: immediate arc slash
        double damage = scaledDamage(ctx, player, 1.20);
        SkillHitboxHelper.arc(player, 2.5, 120).forEach(t -> dealDamage(player, t, damage));

        // 방어 고리(자기 중심 원) + 반격 호 + 방패음
        spawnParticleCircle(player, Particle.DUST, GUARD, 1.5, 24);
        spawnParticleCircle(player, Particle.ENCHANTED_HIT, null, 1.5, 12);
        spawnParticleArc(player, Particle.SWEEP_ATTACK, null, 2.2, 120, 4);
        playSound(player, Sound.ITEM_SHIELD_BLOCK, 1.0f, 1.0f);
        playSound(player, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.8f, 0.9f);
        return true;
    }
}
