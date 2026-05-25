package com.poro.empire.combat.skills.crossbow;

import com.poro.empire.combat.SkillContext;
import com.poro.empire.combat.hitbox.SkillHitboxHelper;
import com.poro.empire.combat.skills.PluginWeaponSkill;
import com.poro.empire.combat.weapon.WeaponType;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public final class CrossbowSniperSkill extends PluginWeaponSkill {
    public CrossbowSniperSkill(Plugin plugin) {
        super(plugin, "crossbow:sniper", "저격태세", WeaponType.CROSSBOW, 20000L);
    }

    @Override
    public boolean execute(Player player, SkillContext ctx) {
        double damage = scaledDamageWithStacks(ctx, player, 4.00, 0.12);
        SkillHitboxHelper.projectileRaycast(player, 50.0, 0.5)
                .ifPresent(t -> dealDamage(player, t, damage));
        consumeStacks(ctx, player);
        return true;
    }
}
