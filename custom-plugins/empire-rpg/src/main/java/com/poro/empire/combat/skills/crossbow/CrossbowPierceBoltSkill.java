package com.poro.empire.combat.skills.crossbow;

import com.poro.empire.combat.SkillContext;
import com.poro.empire.combat.hitbox.SkillHitboxHelper;
import com.poro.empire.combat.skills.PluginWeaponSkill;
import com.poro.empire.combat.weapon.WeaponType;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public final class CrossbowPierceBoltSkill extends PluginWeaponSkill {
    public CrossbowPierceBoltSkill(Plugin plugin) {
        super(plugin, "crossbow:pierce_bolt", "관통볼트", WeaponType.CROSSBOW, 10000L);
    }

    @Override
    public boolean execute(Player player, SkillContext ctx) {
        double damage = scaledDamage(ctx, player, 2.20);
        boolean[] hit = {false};
        SkillHitboxHelper.projectilePierceRaycast(player, 30.0, 0.5)
                .forEach(t -> { dealDamage(player, t, damage); hit[0] = true; });
        if (hit[0]) gainStack(ctx, player, 3);
        return true;
    }
}
