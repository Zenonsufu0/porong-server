package com.poro.empire.combat.skills.scythe;

import com.poro.empire.combat.SkillContext;
import com.poro.empire.combat.hitbox.SkillHitboxHelper;
import com.poro.empire.combat.skills.BaseWeaponSkill;
import com.poro.empire.combat.weapon.WeaponType;
import org.bukkit.entity.Player;

public final class ScytheDeathSlashSkill extends BaseWeaponSkill {
    public ScytheDeathSlashSkill() {
        super("scythe:death_slash", "사신베기", WeaponType.SCYTHE, 4000L);
    }

    @Override
    public boolean execute(Player player, SkillContext ctx) {
        double damage = scaledDamageWithStacks(ctx, player, 1.80, 0.05);
        SkillHitboxHelper.arc(player, 3.0, 150).forEach(t -> dealDamage(player, t, damage));
        return true;
    }
}
