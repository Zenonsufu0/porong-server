package com.poro.empire.combat.skills;

import com.poro.empire.combat.SkillContext;
import com.poro.empire.combat.WeaponSkill;
import com.poro.empire.combat.weapon.WeaponType;
import org.bukkit.entity.Player;

public abstract class BaseStubWeaponSkill implements WeaponSkill {
    private final String key;
    private final String displayName;
    private final WeaponType weaponType;
    private final long cooldown;

    protected BaseStubWeaponSkill(String key, String displayName, WeaponType weaponType, long cooldown) {
        this.key = key;
        this.displayName = displayName;
        this.weaponType = weaponType;
        this.cooldown = cooldown;
    }

    @Override
    public final String key() {
        return key;
    }

    @Override
    public final String displayName() {
        return displayName;
    }

    @Override
    public final WeaponType weaponType() {
        return weaponType;
    }

    @Override
    public final long cooldown() {
        return cooldown;
    }

    @Override
    public boolean execute(Player player, SkillContext context) {
        player.sendMessage("[" + displayName + "] 발동!");
        return true;
    }
}
