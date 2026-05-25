package com.poro.empire.combat.skills.axe;

import com.poro.empire.combat.SkillContext;
import com.poro.empire.combat.hitbox.SkillHitboxHelper;
import com.poro.empire.combat.skills.BaseWeaponSkill;
import com.poro.empire.combat.weapon.WeaponType;
import org.bukkit.entity.Player;

public final class AxeSmashSkill extends BaseWeaponSkill {
    public AxeSmashSkill() {
        super("axe:smash", "철퇴강타", WeaponType.AXE, 4000L);
    }

    @Override
    public boolean execute(Player player, SkillContext ctx) {
        double damage = scaledDamageWithStacks(ctx, player, 2.10, 0.08);
        SkillHitboxHelper.arc(player, 2.5, 100).forEach(t -> dealDamage(player, t, damage));
        gainStack(ctx, player, 3);
        return true;
    }
}
