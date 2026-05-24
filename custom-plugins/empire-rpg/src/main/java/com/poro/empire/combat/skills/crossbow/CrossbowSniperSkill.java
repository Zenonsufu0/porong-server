package com.poro.empire.combat.skills.crossbow;

import com.poro.empire.combat.skills.PluginStubWeaponSkill;
import com.poro.empire.combat.weapon.WeaponType;
import org.bukkit.plugin.Plugin;

public final class CrossbowSniperSkill extends PluginStubWeaponSkill {
    public CrossbowSniperSkill(Plugin plugin) {
        super(plugin, "crossbow:sniper", "저격태세", WeaponType.CROSSBOW, 20000L);
    }
}
