package com.poro.empire.combat.skills.axe;

import com.poro.empire.combat.SkillContext;
import com.poro.empire.combat.hitbox.SkillHitboxHelper;
import com.poro.empire.combat.skills.BaseWeaponSkill;
import com.poro.empire.combat.weapon.WeaponType;
import org.bukkit.entity.Player;

public final class AxeColossalDropSkill extends BaseWeaponSkill {
    public AxeColossalDropSkill() {
        super("axe:colossal_drop", "거신추락", WeaponType.AXE, 18000L);
    }

    @Override
    public boolean execute(Player player, SkillContext ctx) {
        double damage = scaledDamageWithStacks(ctx, player, 4.20, 0.10);
        SkillHitboxHelper.burst(player, 4.5).forEach(t -> dealDamage(player, t, damage));
        consumeStacks(ctx, player);
        return true;
    }
}
