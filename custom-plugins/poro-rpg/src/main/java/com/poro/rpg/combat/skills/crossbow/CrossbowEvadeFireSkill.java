package com.poro.rpg.combat.skills.crossbow;

import com.poro.rpg.combat.SkillContext;
import com.poro.rpg.combat.hitbox.SkillHitboxHelper;
import com.poro.rpg.combat.skills.PluginWeaponSkill;
import com.poro.rpg.combat.weapon.WeaponType;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public final class CrossbowEvadeFireSkill extends PluginWeaponSkill {
    // pt:crossbow_evade_shot — 회피 후 녹색 사격
    private static final Particle.DustOptions BOLT = new Particle.DustOptions(Color.fromRGB(120, 255, 130), 0.9f);

    public CrossbowEvadeFireSkill(Plugin plugin) {
        super(plugin, "crossbow:evade_fire", "회피사격", WeaponType.CROSSBOW, 5000L);
    }

    @Override
    public boolean execute(Player player, SkillContext ctx) {
        dashBackward(player, 2.5);
        double damage = scaledDamage(ctx, player, 1.70);
        SkillHitboxHelper.projectileRaycast(player, 25.0, 0.5)
                .ifPresent(t -> { dealDamage(ctx, player, t, damage); gainStack(ctx, player, 3); });

        // 백스텝 휘익음 + 녹색 빔
        spawnBeam(player, Particle.DUST, BOLT, 25.0, 0.5);
        playSound(player, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.5f, 1.7f);
        playSound(player, Sound.ITEM_CROSSBOW_SHOOT, 1.0f, 1.2f);
        return true;
    }
}
