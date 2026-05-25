package com.poro.empire.combat.skills.sword;

import com.poro.empire.combat.SkillContext;
import com.poro.empire.combat.hitbox.SkillHitboxHelper;
import com.poro.empire.combat.skills.BaseWeaponSkill;
import com.poro.empire.combat.weapon.WeaponType;
import org.bukkit.entity.Player;

public final class SwordFlashSlashSkill extends BaseWeaponSkill {
    public SwordFlashSlashSkill() {
        super("sword:flash_slash", "섬광베기", WeaponType.SWORD, 3000L);
    }

    @Override
    public boolean execute(Player player, SkillContext ctx) {
        dashForward(player, 2.0);
        double damage = scaledDamageWithStacks(ctx, player, 1.60, 0.08);
        SkillHitboxHelper.arc(player, 2.5, 120).forEach(t -> dealDamage(player, t, damage));
        gainStack(ctx, player, 3);
        return true;
    }
}
