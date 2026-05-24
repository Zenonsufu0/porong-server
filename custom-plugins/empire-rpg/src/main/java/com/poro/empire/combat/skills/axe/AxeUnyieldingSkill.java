package com.poro.empire.combat.skills.axe;

import com.poro.empire.combat.skills.PluginStubWeaponSkill;
import com.poro.empire.combat.weapon.WeaponType;
import org.bukkit.plugin.Plugin;

public final class AxeUnyieldingSkill extends PluginStubWeaponSkill {
    public AxeUnyieldingSkill(Plugin plugin) {
        super(plugin, "axe:unyielding", "불굴자세", WeaponType.AXE, 12000L);
    }
}
