package com.poro.empire.combat.skills.sword;

import com.poro.empire.combat.SkillContext;
import com.poro.empire.combat.hitbox.SkillHitboxHelper;
import com.poro.empire.combat.skills.BaseWeaponSkill;
import com.poro.empire.combat.weapon.WeaponType;
import org.bukkit.entity.Player;

public final class SwordFinalStrikeSkill extends BaseWeaponSkill {
    public SwordFinalStrikeSkill() {
        super("sword:final_strike", "결전일섬", WeaponType.SWORD, 16000L);
    }

    @Override
    public boolean execute(Player player, SkillContext ctx) {
        double damage = scaledDamageWithStacks(ctx, player, 3.20, 0.15);
        SkillHitboxHelper.line(player, 6.0, 0.6).forEach(t -> dealDamage(player, t, damage));
        consumeStacks(ctx, player);
        return true;
    }
}
