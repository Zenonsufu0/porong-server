package com.poro.empire.combat.skills.crossbow;

import com.poro.empire.combat.skills.PluginStubWeaponSkill;
import com.poro.empire.combat.weapon.WeaponType;
import org.bukkit.plugin.Plugin;

public final class CrossbowPierceBoltSkill extends PluginStubWeaponSkill {
    public CrossbowPierceBoltSkill(Plugin plugin) {
        super(plugin, "crossbow:pierce_bolt", "관통볼트", WeaponType.CROSSBOW, 10000L);
    }
}
