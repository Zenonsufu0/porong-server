package com.poro.empire.combat.skills.staff;

import com.poro.empire.combat.skills.PluginStubWeaponSkill;
import com.poro.empire.combat.weapon.WeaponType;
import org.bukkit.plugin.Plugin;

public final class StaffElementalBurstSkill extends PluginStubWeaponSkill {
    public StaffElementalBurstSkill(Plugin plugin) {
        super(plugin, "staff:elemental_burst", "속성폭발", WeaponType.STAFF, 8000L);
    }
}
