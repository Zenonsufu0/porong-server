package com.poro.empire.combat.skills.spear;

import com.poro.empire.combat.SkillContext;
import com.poro.empire.combat.hitbox.SkillHitboxHelper;
import com.poro.empire.combat.skills.BaseWeaponSkill;
import com.poro.empire.combat.weapon.WeaponType;
import org.bukkit.entity.Player;

public final class SpearChargeSkill extends BaseWeaponSkill {
    public SpearChargeSkill() {
        super("spear:charge", "돌파창", WeaponType.SPEAR, 9000L);
    }

    @Override
    public boolean execute(Player player, SkillContext ctx) {
        dashForward(player, 5.0);
        double damage = scaledDamage(ctx, player, 2.60);
        SkillHitboxHelper.line(player, 5.0, 1.0).forEach(t -> dealDamage(player, t, damage));
        return true;
    }
}
