package com.poro.empire.combat.skills.spear;

import com.poro.empire.combat.skills.BaseStubWeaponSkill;
import com.poro.empire.combat.weapon.WeaponType;

public final class SpearThrustSkill extends BaseStubWeaponSkill {
    public SpearThrustSkill() {
        super("spear:thrust", "관통찌르기", WeaponType.SPEAR, 4000L);
    }
}
