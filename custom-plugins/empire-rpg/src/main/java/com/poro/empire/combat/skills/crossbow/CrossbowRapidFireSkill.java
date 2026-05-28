package com.poro.empire.combat.skills.crossbow;

import com.poro.empire.combat.SkillContext;
import com.poro.empire.combat.hitbox.SkillHitboxHelper;
import com.poro.empire.combat.skills.PluginWeaponSkill;
import com.poro.empire.combat.weapon.WeaponType;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public final class CrossbowRapidFireSkill extends PluginWeaponSkill {
    public CrossbowRapidFireSkill(Plugin plugin) {
        super(plugin, "crossbow:rapid_fire", "속사", WeaponType.CROSSBOW, 3000L);
    }

    @Override
    public boolean execute(Player player, SkillContext ctx) {
        double damage = scaledDamage(ctx, player, 0.75);
        boolean[] hit = {false};
        for (int i = 0; i < 3; i++) {
            SkillHitboxHelper.projectileRaycast(player, 20.0, 0.5)
                    .ifPresent(t -> { dealDamage(player, t, damage); hit[0] = true; });
        }
        if (hit[0]) gainStack(ctx, player, 3);
        return true;
    }
}
