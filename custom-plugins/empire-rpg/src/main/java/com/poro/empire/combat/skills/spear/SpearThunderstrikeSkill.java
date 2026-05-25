package com.poro.empire.combat.skills.spear;

import com.poro.empire.combat.SkillContext;
import com.poro.empire.combat.hitbox.SkillHitboxHelper;
import com.poro.empire.combat.skills.BaseWeaponSkill;
import com.poro.empire.combat.weapon.WeaponType;
import org.bukkit.entity.Player;

public final class SpearThunderstrikeSkill extends BaseWeaponSkill {
    public SpearThunderstrikeSkill() {
        super("spear:thunderstrike", "천뢰일창", WeaponType.SPEAR, 18000L);
    }

    @Override
    public boolean execute(Player player, SkillContext ctx) {
        double damage = scaledDamageWithStacks(ctx, player, 3.60, 0.08);
        SkillHitboxHelper.line(player, 9.0, 0.8).forEach(t -> dealDamage(player, t, damage));
        consumeStacks(ctx, player);
        return true;
    }
}
