package com.poro.empire.combat.skills.axe;

import com.poro.empire.combat.SkillContext;
import com.poro.empire.combat.hitbox.SkillHitboxHelper;
import com.poro.empire.combat.skills.BaseWeaponSkill;
import com.poro.empire.combat.weapon.WeaponType;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

public final class AxeSmashSkill extends BaseWeaponSkill {
    // pt:axe_heavy_strike — 호박빛 중량 강타
    private static final Particle.DustOptions AMBER = new Particle.DustOptions(Color.fromRGB(220, 140, 40), 1.3f);

    public AxeSmashSkill() {
        super("axe:smash", "철퇴강타", WeaponType.AXE, 4000L);
    }

    @Override
    public boolean execute(Player player, SkillContext ctx) {
        double damage = scaledDamageWithStacks(ctx, player, 2.10, 0.08);
        var targets = SkillHitboxHelper.arc(player, 2.5, 100);
        targets.forEach(t -> dealDamage(ctx, player, t, damage));
        if (!targets.isEmpty()) gainStack(ctx, player, 3);   // 명중 시에만 충전 (정본 §4)

        // 묵직한 호박빛 호 + 타격 스파크 + 둔중한 타격음
        spawnParticleArc(player, Particle.DUST, AMBER, 2.5, 100, 12);
        spawnParticleArc(player, Particle.CRIT, null, 2.3, 100, 8);
        playSound(player, Sound.ENTITY_PLAYER_ATTACK_CRIT, 1.0f, 0.8f);
        return true;
    }
}
