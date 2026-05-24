package com.poro.empire.combat.skills.crossbow;

import com.poro.empire.combat.skills.PluginStubWeaponSkill;
import com.poro.empire.combat.weapon.WeaponType;
import org.bukkit.plugin.Plugin;

public final class CrossbowEvadeFireSkill extends PluginStubWeaponSkill {
    public CrossbowEvadeFireSkill(Plugin plugin) {
        super(plugin, "crossbow:evade_fire", "회피사격", WeaponType.CROSSBOW, 6000L);
    }
}
