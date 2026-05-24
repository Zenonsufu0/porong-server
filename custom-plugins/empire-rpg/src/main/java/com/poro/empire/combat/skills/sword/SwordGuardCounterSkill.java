package com.poro.empire.combat.skills.sword;

import com.poro.empire.combat.skills.PluginStubWeaponSkill;
import com.poro.empire.combat.weapon.WeaponType;
import org.bukkit.plugin.Plugin;

public final class SwordGuardCounterSkill extends PluginStubWeaponSkill {
    public SwordGuardCounterSkill(Plugin plugin) {
        super(plugin, "sword:guard_counter", "수호반격", WeaponType.SWORD, 10000L);
    }
}
