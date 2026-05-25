package com.poro.empire.combat.skills.staff;

import com.poro.empire.combat.SkillContext;
import com.poro.empire.combat.hitbox.SkillHitboxHelper;
import com.poro.empire.combat.skills.BaseWeaponSkill;
import com.poro.empire.combat.weapon.WeaponType;
import org.bukkit.entity.Player;

public final class StaffArcaneRushSkill extends BaseWeaponSkill {
    public StaffArcaneRushSkill() {
        super("staff:arcane_rush", "마력쇄도", WeaponType.STAFF, 10000L);
    }

    @Override
    public boolean execute(Player player, SkillContext ctx) {
        double damage = scaledDamage(ctx, player, 2.00);
        SkillHitboxHelper.burst(player, 4.0).forEach(t -> dealDamage(player, t, damage));
        dashBackward(player, 1.5);
        gainStack(ctx, player, 5);
        return true;
    }
}
