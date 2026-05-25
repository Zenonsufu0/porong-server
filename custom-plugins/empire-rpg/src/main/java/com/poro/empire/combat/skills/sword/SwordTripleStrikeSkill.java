package com.poro.empire.combat.skills.sword;

import com.poro.empire.combat.SkillContext;
import com.poro.empire.combat.hitbox.SkillHitboxHelper;
import com.poro.empire.combat.skills.PluginWeaponSkill;
import com.poro.empire.combat.weapon.WeaponType;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public final class SwordTripleStrikeSkill extends PluginWeaponSkill {
    public SwordTripleStrikeSkill(Plugin plugin) {
        super(plugin, "sword:triple_strike", "연속참", WeaponType.SWORD, 6000L);
    }

    @Override
    public boolean execute(Player player, SkillContext ctx) {
        double damage = scaledDamage(ctx, player, 0.70);
        for (int i = 0; i < 3; i++) {
            SkillHitboxHelper.arc(player, 2.5, 120).forEach(t -> dealDamage(player, t, damage));
        }
        return true;
    }
}
