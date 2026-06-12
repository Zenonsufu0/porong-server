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

public final class CrossbowPierceBoltSkill extends PluginWeaponSkill {
    // pt:piercing_bolt — 관통 전격 볼트
    private static final Particle.DustOptions BOLT = new Particle.DustOptions(Color.fromRGB(120, 255, 130), 1.0f);

    public CrossbowPierceBoltSkill(Plugin plugin) {
        super(plugin, "crossbow:pierce_bolt", "관통볼트", WeaponType.CROSSBOW, 8000L);
    }

    @Override
    public boolean execute(Player player, SkillContext ctx) {
        double damage = scaledDamage(ctx, player, 2.00);
        boolean[] hit = {false};
        SkillHitboxHelper.projectilePierceRaycast(player, 30.0, 0.5)
                .forEach(t -> { dealDamage(ctx, player, t, damage); hit[0] = true; });
        if (hit[0]) gainStack(ctx, player, 3);

        // 길고 굵은 관통 빔 + 전격 스파크
        spawnBeam(player, Particle.DUST, BOLT, 30.0, 0.4);
        spawnBeam(player, Particle.ELECTRIC_SPARK, null, 30.0, 1.0);
        playSound(player, Sound.ITEM_CROSSBOW_SHOOT, 1.0f, 0.9f);
        return true;
    }
}
