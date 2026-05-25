package com.poro.empire.combat.skills.staff;

import com.poro.empire.combat.SkillContext;
import com.poro.empire.combat.hitbox.SkillHitboxHelper;
import com.poro.empire.combat.skills.PluginWeaponSkill;
import com.poro.empire.combat.weapon.WeaponType;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public final class StaffStarburstSkill extends PluginWeaponSkill {
    public StaffStarburstSkill(Plugin plugin) {
        super(plugin, "staff:starburst", "별빛쇄도", WeaponType.STAFF, 20000L);
    }

    @Override
    public boolean execute(Player player, SkillContext ctx) {
        double damage = scaledDamageWithStacks(ctx, player, 3.80, 0.10);
        SkillHitboxHelper.projectileRaycast(player, 22.0, 0.5)
                .ifPresent(t -> dealDamage(player, t, damage));
        consumeStacks(ctx, player);
        return true;
    }
}
