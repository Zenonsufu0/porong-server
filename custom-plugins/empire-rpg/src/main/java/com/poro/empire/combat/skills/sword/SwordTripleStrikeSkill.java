package com.poro.empire.combat.skills.sword;

import com.poro.empire.combat.skills.PluginStubWeaponSkill;
import com.poro.empire.combat.weapon.WeaponType;
import org.bukkit.plugin.Plugin;

public final class SwordTripleStrikeSkill extends PluginStubWeaponSkill {
    public SwordTripleStrikeSkill(Plugin plugin) {
        super(plugin, "sword:triple_strike", "연속참", WeaponType.SWORD, 6000L);
    }
}
