package com.poro.empire.combat.skills.axe;

import com.poro.empire.combat.SkillContext;
import com.poro.empire.combat.hitbox.SkillHitboxHelper;
import com.poro.empire.combat.skills.BaseWeaponSkill;
import com.poro.empire.combat.weapon.WeaponType;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

public final class AxeCrushChargeSkill extends BaseWeaponSkill {
    // pt:axe_crush_charge — 파쇄 돌진 궤적
    private static final Particle.DustOptions AMBER = new Particle.DustOptions(Color.fromRGB(220, 140, 40), 1.3f);

    public AxeCrushChargeSkill() {
        super("axe:crush_charge", "파쇄돌진", WeaponType.AXE, 5000L);
    }

    @Override
    public boolean execute(Player player, SkillContext ctx) {
        dashForward(player, 3.0);
        double damage = scaledDamage(ctx, player, 2.80);
        SkillHitboxHelper.line(player, 4.0, 0.8).forEach(t -> dealDamage(player, t, damage));

        // 파쇄 궤적 직선 + 분진 구름 + 추진음
        spawnParticleLine(player, Particle.DUST, AMBER, 4.0, 22);
        spawnParticleLine(player, Particle.CLOUD, null, 4.0, 12);
        playSound(player, Sound.ENTITY_PLAYER_ATTACK_KNOCKBACK, 1.0f, 0.8f);
        return true;
    }
}
