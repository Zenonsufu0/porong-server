package com.poro.empire.combat.skills.sword;

import com.poro.empire.combat.skills.BaseStubWeaponSkill;
import com.poro.empire.combat.weapon.WeaponType;

public final class SwordFlashSlashSkill extends BaseStubWeaponSkill {
    public SwordFlashSlashSkill() {
        super("sword:flash_slash", "섬광베기", WeaponType.SWORD, 3000L);
    }
}
