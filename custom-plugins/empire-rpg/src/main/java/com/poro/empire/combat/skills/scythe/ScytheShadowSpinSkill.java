package com.poro.empire.combat.skills.scythe;

import com.poro.empire.combat.SkillContext;
import com.poro.empire.combat.hitbox.SkillHitboxHelper;
import com.poro.empire.combat.skills.PluginWeaponSkill;
import com.poro.empire.combat.weapon.WeaponType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.List;

public final class ScytheShadowSpinSkill extends PluginWeaponSkill {
    public ScytheShadowSpinSkill(Plugin plugin) {
        super(plugin, "scythe:shadow_spin", "월영회전", WeaponType.SCYTHE, 5000L);
    }

    @Override
    public boolean execute(Player player, SkillContext ctx) {
        dashSideways(player, 2.5);
        double damage = scaledDamage(ctx, player, 0.60);
        boolean hitAny = false;
        for (int i = 0; i < 4; i++) {
            List<LivingEntity> targets = SkillHitboxHelper.burst(player, 3.0);
            if (!targets.isEmpty()) {
                targets.forEach(t -> dealDamage(player, t, damage));
                hitAny = true;
            }
        }
        if (hitAny) gainStack(ctx, player, 3);
        return true;
    }
}
