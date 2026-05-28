package com.poro.empire.combat.skills.spear;

import com.poro.empire.combat.SkillContext;
import com.poro.empire.combat.hitbox.SkillHitboxHelper;
import com.poro.empire.combat.skills.BaseWeaponSkill;
import com.poro.empire.combat.weapon.WeaponType;
import org.bukkit.entity.Player;

public final class SpearCrescentSkill extends BaseWeaponSkill {
    public SpearCrescentSkill() {
        super("spear:crescent", "반월창", WeaponType.SPEAR, 4000L);
    }

    @Override
    public boolean execute(Player player, SkillContext ctx) {
        double damage = scaledDamage(ctx, player, 1.90);
        SkillHitboxHelper.arc(player, 3.0, 150).forEach(t -> dealDamage(player, t, damage));
        return true;
    }
}
