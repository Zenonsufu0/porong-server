package com.poro.empire.combat.skills.axe;

import com.poro.empire.combat.SkillContext;
import com.poro.empire.combat.hitbox.SkillHitboxHelper;
import com.poro.empire.combat.skills.BaseWeaponSkill;
import com.poro.empire.combat.weapon.WeaponType;
import org.bukkit.entity.Player;

public final class AxeCrushChargeSkill extends BaseWeaponSkill {
    public AxeCrushChargeSkill() {
        super("axe:crush_charge", "파쇄돌진", WeaponType.AXE, 5000L);
    }

    @Override
    public boolean execute(Player player, SkillContext ctx) {
        dashForward(player, 3.0);
        double damage = scaledDamage(ctx, player, 2.80);
        SkillHitboxHelper.line(player, 4.0, 0.8).forEach(t -> dealDamage(player, t, damage));
        return true;
    }
}
