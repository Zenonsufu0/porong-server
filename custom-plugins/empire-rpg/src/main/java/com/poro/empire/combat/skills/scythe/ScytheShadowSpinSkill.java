package com.poro.empire.combat.skills.scythe;

import com.poro.empire.combat.skills.PluginStubWeaponSkill;
import com.poro.empire.combat.weapon.WeaponType;
import org.bukkit.plugin.Plugin;

public final class ScytheShadowSpinSkill extends PluginStubWeaponSkill {
    public ScytheShadowSpinSkill(Plugin plugin) {
        super(plugin, "scythe:shadow_spin", "월영회전", WeaponType.SCYTHE, 5000L);
    }
}
