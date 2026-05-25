package com.poro.empire.combat.skills.scythe;

import com.poro.empire.combat.SkillContext;
import com.poro.empire.combat.hitbox.SkillHitboxHelper;
import com.poro.empire.combat.skills.BaseWeaponSkill;
import com.poro.empire.combat.weapon.WeaponType;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;

public final class ScytheExecutionSkill extends BaseWeaponSkill {
    public ScytheExecutionSkill() {
        super("scythe:execution", "처형낫", WeaponType.SCYTHE, 16000L);
    }

    @Override
    public boolean execute(Player player, SkillContext ctx) {
        consumeStacks(ctx, player);
        SkillHitboxHelper.line(player, 4.0, 1.0).forEach(t -> {
            var maxHpAttr = t.getAttribute(Attribute.MAX_HEALTH);
            double hpRatio = (maxHpAttr != null && maxHpAttr.getValue() > 0)
                    ? t.getHealth() / maxHpAttr.getValue() : 1.0;
            double coeff = hpRatio < 0.30 ? 4.80 : 2.80;
            dealDamage(player, t, scaledDamage(ctx, player, coeff));
        });
        return true;
    }
}
