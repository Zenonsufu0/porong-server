package com.poro.rpg.combat.skills.staff;

import com.poro.rpg.combat.SkillContext;
import com.poro.rpg.combat.hitbox.SkillHitboxHelper;
import com.poro.rpg.combat.skills.BaseWeaponSkill;
import com.poro.rpg.combat.weapon.WeaponType;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

public final class StaffArcaneRushSkill extends BaseWeaponSkill {
    // pt:staff_magic_surge — 비전 폭발 노바 (자기 중심)
    private static final Particle.DustOptions ARCANE = new Particle.DustOptions(Color.fromRGB(160, 110, 255), 1.2f);

    public StaffArcaneRushSkill() {
        super("staff:arcane_rush", "마력쇄도", WeaponType.STAFF, 11000L);
    }

    @Override
    public boolean execute(Player player, SkillContext ctx) {
        double damage = scaledDamage(ctx, player, 2.40);
        SkillHitboxHelper.burst(player, 4.0).forEach(t -> dealDamage(ctx, player, t, damage));
        dashBackward(player, 1.5);
        // 충전 없음 — 스태프 마력 충전은 LMB 마력탄 명중만 (정본 §4, DL-125)

        // 자기 중심 비전 노바 2중 고리 + 시전음
        spawnParticleCircle(player, Particle.DUST, ARCANE, 4.0, 40);
        spawnParticleCircle(player, Particle.WITCH, null, 3.0, 20);
        playSound(player, Sound.ENTITY_EVOKER_CAST_SPELL, 1.0f, 0.8f);
        playSound(player, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 1.0f);
        return true;
    }
}
