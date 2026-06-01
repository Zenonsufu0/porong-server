package com.poro.empire.combat.skills.spear;

import com.poro.empire.combat.SkillContext;
import com.poro.empire.combat.hitbox.SkillHitboxHelper;
import com.poro.empire.combat.skills.BaseWeaponSkill;
import com.poro.empire.combat.weapon.WeaponType;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

public final class SpearThunderstrikeSkill extends BaseWeaponSkill {
    // pt:thunder_spear_line — 천뢰, 전격 직선
    private static final Particle.DustOptions ELECTRIC = new Particle.DustOptions(Color.fromRGB(150, 210, 255), 1.2f);

    public SpearThunderstrikeSkill() {
        super("spear:thunderstrike", "천뢰일창", WeaponType.SPEAR, 15000L);
    }

    @Override
    public boolean execute(Player player, SkillContext ctx) {
        double damage = scaledDamageFullChargeSpike(ctx, player, 2.96, 0.04, 1.20);
        SkillHitboxHelper.line(player, 9.0, 0.8).forEach(t -> dealDamage(ctx, player, t, damage));
        consumeStacks(ctx, player);

        // 긴 전격 직선 + 스파크 + 천둥음
        spawnParticleLine(player, Particle.DUST, ELECTRIC, 9.0, 40);
        spawnParticleLine(player, Particle.ELECTRIC_SPARK, null, 9.0, 28);
        playSound(player, Sound.ITEM_TRIDENT_THUNDER, 1.0f, 1.2f);
        playSound(player, Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 0.7f, 1.4f);
        return true;
    }
}
