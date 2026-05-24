package com.poro.empire.combat.skills.staff;

import com.poro.empire.combat.skills.PluginStubWeaponSkill;
import com.poro.empire.combat.weapon.WeaponType;
import org.bukkit.plugin.Plugin;

public final class StaffStarburstSkill extends PluginStubWeaponSkill {
    public StaffStarburstSkill(Plugin plugin) {
        super(plugin, "staff:starburst", "별빛쇄도", WeaponType.STAFF, 20000L);
    }
}
