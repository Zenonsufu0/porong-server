package com.poro.empire.combat.skills.staff;

import com.poro.empire.combat.SkillContext;
import com.poro.empire.combat.hitbox.SkillHitboxHelper;
import com.poro.empire.combat.skills.PluginWeaponSkill;
import com.poro.empire.combat.weapon.WeaponType;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public final class StaffArcaneOrbSkill extends PluginWeaponSkill {
    public StaffArcaneOrbSkill(Plugin plugin) {
        super(plugin, "staff:arcane_orb", "마력탄", WeaponType.STAFF, 3000L);
    }

    @Override
    public boolean execute(Player player, SkillContext ctx) {
        double damage = scaledDamage(ctx, player, 1.50);
        SkillHitboxHelper.projectileRaycast(player, 20.0, 0.5)
                .ifPresent(t -> dealDamage(player, t, damage));
        gainStack(ctx, player, 5);
        return true;
    }
}
