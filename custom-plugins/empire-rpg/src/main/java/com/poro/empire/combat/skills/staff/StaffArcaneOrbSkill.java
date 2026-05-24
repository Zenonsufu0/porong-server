package com.poro.empire.combat.skills.staff;

import com.poro.empire.combat.skills.PluginStubWeaponSkill;
import com.poro.empire.combat.weapon.WeaponType;
import org.bukkit.plugin.Plugin;

public final class StaffArcaneOrbSkill extends PluginStubWeaponSkill {
    public StaffArcaneOrbSkill(Plugin plugin) {
        super(plugin, "staff:arcane_orb", "마력탄", WeaponType.STAFF, 3000L);
    }
}
