package com.poro.empire.combat.skills.crossbow;

import com.poro.empire.combat.SkillContext;
import com.poro.empire.combat.hitbox.SkillHitboxHelper;
import com.poro.empire.combat.skills.PluginWeaponSkill;
import com.poro.empire.combat.weapon.WeaponType;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public final class CrossbowSniperSkill extends PluginWeaponSkill {
    // pt:sniper_bolt — 장거리 정밀 저격 빔
    private static final Particle.DustOptions BOLT = new Particle.DustOptions(Color.fromRGB(120, 255, 130), 1.0f);

    public CrossbowSniperSkill(Plugin plugin) {
        super(plugin, "crossbow:sniper", "저격태세", WeaponType.CROSSBOW, 14000L);
    }

    @Override
    public boolean execute(Player player, SkillContext ctx) {
        double damage = scaledDamageFullChargeSpike(ctx, player, 3.35, 0.05, 1.20);
        SkillHitboxHelper.projectileRaycast(player, 50.0, 0.5)
                .ifPresent(t -> dealDamage(ctx, player, t, damage));
        consumeStacks(ctx, player);

        // 50블록 정밀 빔 (녹색 + 흰 잔광) + 묵직한 저격음
        spawnBeam(player, Particle.DUST, BOLT, 50.0, 0.4);
        spawnBeam(player, Particle.END_ROD, null, 50.0, 1.5);
        playSound(player, Sound.ITEM_CROSSBOW_SHOOT, 1.0f, 0.7f);
        playSound(player, Sound.ENTITY_ARROW_HIT_PLAYER, 1.0f, 0.8f);
        return true;
    }
}
