package com.poro.rpg.combat.skills.spear;

import com.poro.rpg.combat.SkillContext;
import com.poro.rpg.combat.hitbox.SkillHitboxHelper;
import com.poro.rpg.combat.skills.BaseWeaponSkill;
import com.poro.rpg.combat.weapon.WeaponType;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

public final class SpearThunderstrikeSkill extends BaseWeaponSkill {
    // pt:thunder_spear_line — 천뢰, 전격 직선
    private static final Particle.DustOptions ELECTRIC = new Particle.DustOptions(Color.fromRGB(150, 210, 255), 1.2f);

    public SpearThunderstrikeSkill() {
        // DL-129 추가#28: F 회전율 정렬 — 쿨 15s→10s(3스택 적립 ~9s), 계수 ×0.667 DPS 중립.
        super("spear:thunderstrike", "천뢰일창", WeaponType.SPEAR, 10000L);
    }

    @Override
    public boolean execute(Player player, SkillContext ctx) {
        double damage = scaledDamageFullChargeSpike(ctx, player, 1.97, 0.03, 1.20);
        SkillHitboxHelper.line(player, 9.0, 0.8).forEach(t -> dealDamage(ctx, player, t, damage));
        consumeStacks(ctx, player);
        ctx.effectDisplay().spawnGroundTravel(400102, player, 9.0, 4.0f, 9, 0.6);   // 천뢰일창 (바닥 비행)

        // 긴 전격 직선 + 스파크 + 천둥음
        spawnParticleLine(player, Particle.DUST, ELECTRIC, 9.0, 40);
        spawnParticleLine(player, Particle.ELECTRIC_SPARK, null, 9.0, 28);
        playSound(player, Sound.ITEM_TRIDENT_THUNDER, 1.0f, 1.2f);
        playSound(player, Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 0.7f, 1.4f);
        return true;
    }
}
