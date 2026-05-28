package com.poro.empire.combat.skills.staff;

import com.poro.empire.combat.SkillContext;
import com.poro.empire.combat.hitbox.SkillHitboxHelper;
import com.poro.empire.combat.skills.PluginWeaponSkill;
import com.poro.empire.combat.weapon.WeaponType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public final class StaffElementalBurstSkill extends PluginWeaponSkill {
    public StaffElementalBurstSkill(Plugin plugin) {
        super(plugin, "staff:elemental_burst", "속성폭발", WeaponType.STAFF, 6000L);
    }

    @Override
    public boolean execute(Player player, SkillContext ctx) {
        double damage = scaledDamage(ctx, player, 2.40);
        SkillHitboxHelper.projectileRaycast(player, 18.0, 0.5).ifPresent(primary -> {
            dealDamage(player, primary, damage);
            // AoE burst at impact location
            primary.getWorld().getNearbyLivingEntities(primary.getLocation(), 2.5).stream()
                    .filter(e -> !e.equals(player) && !e.equals(primary))
                    .forEach((LivingEntity splash) -> dealDamage(player, splash, damage));
        });
        gainStack(ctx, player, 5);
        return true;
    }
}
