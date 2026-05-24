package com.poro.empire.combat.skills.crossbow;

import com.poro.empire.combat.skills.PluginStubWeaponSkill;
import com.poro.empire.combat.weapon.WeaponType;
import org.bukkit.plugin.Plugin;

public final class CrossbowRapidFireSkill extends PluginStubWeaponSkill {
    public CrossbowRapidFireSkill(Plugin plugin) {
        super(plugin, "crossbow:rapid_fire", "속사", WeaponType.CROSSBOW, 3000L);
    }
}
