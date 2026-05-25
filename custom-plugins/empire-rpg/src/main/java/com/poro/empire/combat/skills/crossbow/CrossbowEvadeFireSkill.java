package com.poro.empire.combat.skills.crossbow;

import com.poro.empire.combat.SkillContext;
import com.poro.empire.combat.hitbox.SkillHitboxHelper;
import com.poro.empire.combat.skills.PluginWeaponSkill;
import com.poro.empire.combat.weapon.WeaponType;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public final class CrossbowEvadeFireSkill extends PluginWeaponSkill {
    public CrossbowEvadeFireSkill(Plugin plugin) {
        super(plugin, "crossbow:evade_fire", "회피사격", WeaponType.CROSSBOW, 6000L);
    }

    @Override
    public boolean execute(Player player, SkillContext ctx) {
        dashBackward(player, 2.5);
        double damage = scaledDamage(ctx, player, 1.70);
        SkillHitboxHelper.projectileRaycast(player, 25.0, 0.5)
                .ifPresent(t -> dealDamage(player, t, damage));
        gainStack(ctx, player, 3);
        return true;
    }
}
