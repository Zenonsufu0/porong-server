package com.poro.empire.combat.skills.scythe;

import com.poro.empire.combat.SkillContext;
import com.poro.empire.combat.hitbox.SkillHitboxHelper;
import com.poro.empire.combat.skills.BaseWeaponSkill;
import com.poro.empire.combat.weapon.WeaponType;
import org.bukkit.entity.Player;

public final class ScytheGrimStrikeSkill extends BaseWeaponSkill {
    public ScytheGrimStrikeSkill() {
        super("scythe:grim_strike", "그믐참", WeaponType.SCYTHE, 8000L);
    }

    @Override
    public boolean execute(Player player, SkillContext ctx) {
        int stacks = getStacks(ctx, player);
        double damage = scaledDamageWithStacks(ctx, player, 2.40, 0.08);
        double lifeStealAmount = damage * (0.06 * stacks);
        SkillHitboxHelper.cone(player, 4.0, 60).forEach(t -> {
            dealDamage(player, t, damage);
            if (lifeStealAmount > 0) lifesteal(player, lifeStealAmount);
        });
        return true;
    }
}
