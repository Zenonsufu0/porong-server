package com.poro.rpg.combat.skills;

import com.poro.rpg.combat.weapon.WeaponType;
import org.bukkit.plugin.Plugin;

public abstract class PluginStubWeaponSkill extends BaseStubWeaponSkill {
    protected final Plugin plugin;

    protected PluginStubWeaponSkill(
            Plugin plugin,
            String key,
            String displayName,
            WeaponType weaponType,
            long cooldown
    ) {
        super(key, displayName, weaponType, cooldown);
        this.plugin = plugin;
    }
}
