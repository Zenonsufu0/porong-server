package com.poro.rpg.combat.skills.crossbow;

import com.poro.rpg.combat.SkillContext;
import com.poro.rpg.combat.hitbox.SkillHitboxHelper;
import com.poro.rpg.combat.skills.PluginWeaponSkill;
import com.poro.rpg.combat.weapon.WeaponType;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public final class CrossbowSniperSkill extends PluginWeaponSkill {
    // pt:sniper_bolt — 장거리 정밀 저격 빔
    private static final Particle.DustOptions BOLT = new Particle.DustOptions(Color.fromRGB(120, 255, 130), 1.0f);

    public CrossbowSniperSkill(Plugin plugin) {
        // DL-129 추가#28: F 회전율 정렬 — 쿨 14s→10s(3스택 적립 ~9s, 다슬롯 적립으로 더 빠름), 계수 ×0.714 DPS 중립.
        super(plugin, "crossbow:sniper", "저격태세", WeaponType.CROSSBOW, 10000L);
    }

    @Override
    public boolean execute(Player player, SkillContext ctx) {
        double damage = scaledDamageFullChargeSpike(ctx, player, 2.39, 0.04, 1.20);
        SkillHitboxHelper.projectileRaycast(player, 50.0, 0.5)
                .ifPresent(t -> dealDamage(ctx, player, t, damage));
        consumeStacks(ctx, player);
        ctx.effectDisplay().spawnGroundTravel(400103, player, 24.0, 4.0f, 9, 0.6, true);   // 저격태세 (창처럼 평면 + 조준 추종)

        // 50블록 정밀 빔 (녹색 + 흰 잔광) + 묵직한 저격음
        spawnBeam(player, Particle.DUST, BOLT, 50.0, 0.4);
        spawnBeam(player, Particle.END_ROD, null, 50.0, 1.5);
        playSound(player, Sound.ITEM_CROSSBOW_SHOOT, 1.0f, 0.7f);
        playSound(player, Sound.ENTITY_ARROW_HIT_PLAYER, 1.0f, 0.8f);
        return true;
    }
}
