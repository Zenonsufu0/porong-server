package com.poro.empire.combat.skills.spear;

import com.poro.empire.combat.SkillContext;
import com.poro.empire.combat.hitbox.SkillHitboxHelper;
import com.poro.empire.combat.skills.BaseWeaponSkill;
import com.poro.empire.combat.weapon.WeaponType;
import org.bukkit.entity.Player;

public final class SpearThrustSkill extends BaseWeaponSkill {
    public SpearThrustSkill() {
        super("spear:thrust", "관통찌르기", WeaponType.SPEAR, 3000L);
    }

    @Override
    public boolean execute(Player player, SkillContext ctx) {
        double damage = scaledDamageWithStacks(ctx, player, 1.70, 0.05);
        SkillHitboxHelper.line(player, 5.0, 0.5).forEach(t -> dealDamage(player, t, damage));
        gainStack(ctx, player, 5);
        return true;
    }
}
