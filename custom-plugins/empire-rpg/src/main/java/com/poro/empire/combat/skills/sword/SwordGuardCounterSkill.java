package com.poro.empire.combat.skills.sword;

import com.poro.empire.combat.SkillContext;
import com.poro.empire.combat.hitbox.SkillHitboxHelper;
import com.poro.empire.combat.skills.PluginWeaponSkill;
import com.poro.empire.combat.weapon.WeaponType;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public final class SwordGuardCounterSkill extends PluginWeaponSkill {
    public SwordGuardCounterSkill(Plugin plugin) {
        super(plugin, "sword:guard_counter", "수호반격", WeaponType.SWORD, 10000L);
    }

    @Override
    public boolean execute(Player player, SkillContext ctx) {
        // Guard: brief resistance buff
        player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 40, 2, false, false, true));
        // Counter: immediate arc slash
        double damage = scaledDamage(ctx, player, 1.20);
        SkillHitboxHelper.arc(player, 2.5, 120).forEach(t -> dealDamage(player, t, damage));
        return true;
    }
}
