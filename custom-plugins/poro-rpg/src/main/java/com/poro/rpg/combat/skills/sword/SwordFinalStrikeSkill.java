package com.poro.rpg.combat.skills.sword;

import com.poro.rpg.combat.SkillContext;
import com.poro.rpg.combat.hitbox.SkillHitboxHelper;
import com.poro.rpg.combat.skills.BaseWeaponSkill;
import com.poro.rpg.combat.weapon.WeaponType;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public final class SwordFinalStrikeSkill extends BaseWeaponSkill {
    // pt:sword_final_line — 결전의 일섬, 금빛 직선 베기
    private static final Particle.DustOptions FINAL = new Particle.DustOptions(Color.fromRGB(255, 248, 200), 1.4f);
    private static final double LENGTH = 6.0;

    public SwordFinalStrikeSkill() {
        super("sword:final_strike", "결전일섬", WeaponType.SWORD, 16000L);
    }

    @Override
    public boolean execute(Player player, SkillContext ctx) {
        double damage = scaledDamageFullChargeSpike(ctx, player, 3.32, 0.06, 1.20);
        SkillHitboxHelper.line(player, LENGTH, 0.6).forEach(t -> dealDamage(ctx, player, t, damage));
        consumeStacks(ctx, player);

        // 금빛 직선 빔 + 잔광 + 끝점 임팩트
        spawnParticleLine(player, Particle.DUST, FINAL, LENGTH, 30);
        spawnParticleLine(player, Particle.END_ROD, null, LENGTH, 20);
        Vector dir = player.getLocation().getDirection().setY(0).normalize();
        Location end = player.getLocation().add(0, 1.0, 0).add(dir.multiply(LENGTH));
        spawnImpactEffect(end, Particle.DUST, FINAL, 20);
        spawnImpactEffect(end, Particle.CRIT, null, 12);
        playSound(player, Sound.ENTITY_PLAYER_ATTACK_CRIT, 1.0f, 0.8f);
        playSound(player, Sound.ITEM_TRIDENT_THROW, 1.0f, 0.9f);
        return true;
    }
}
