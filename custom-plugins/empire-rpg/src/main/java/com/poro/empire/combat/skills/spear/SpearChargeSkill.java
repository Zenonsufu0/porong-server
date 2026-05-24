package com.poro.empire.combat.skills.spear;

import com.poro.empire.combat.skills.BaseStubWeaponSkill;
import com.poro.empire.combat.weapon.WeaponType;

public final class SpearChargeSkill extends BaseStubWeaponSkill {
    public SpearChargeSkill() {
        super("spear:charge", "돌파창", WeaponType.SPEAR, 8000L);
    }
}
