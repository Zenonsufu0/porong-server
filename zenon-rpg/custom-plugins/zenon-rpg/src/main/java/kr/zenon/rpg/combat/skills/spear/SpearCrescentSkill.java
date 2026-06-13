package kr.zenon.rpg.combat.skills.spear;

import kr.zenon.rpg.combat.SkillContext;
import kr.zenon.rpg.combat.hitbox.SkillHitboxHelper;
import kr.zenon.rpg.combat.skills.BaseWeaponSkill;
import kr.zenon.rpg.combat.weapon.WeaponType;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

public final class SpearCrescentSkill extends BaseWeaponSkill {
    // pt:spear_halfmoon_arc — 반월 호 (청록 외호 + 흰 내호)
    private static final Particle.DustOptions TEAL = new Particle.DustOptions(Color.fromRGB(90, 220, 210), 1.1f);
    private static final Particle.DustOptions WHITE = new Particle.DustOptions(Color.fromRGB(235, 245, 245), 0.9f);

    public SpearCrescentSkill() {
        super("spear:crescent", "반월창", WeaponType.SPEAR, 4000L);
    }

    @Override
    public boolean execute(Player player, SkillContext ctx) {
        double damage = scaledDamage(ctx, player, 1.90);
        SkillHitboxHelper.arc(player, 3.0, 150).forEach(t -> dealDamage(ctx, player, t, damage));

        // 넓은 반월 2겹 호 + sweep 마크
        spawnParticleArc(player, Particle.DUST, TEAL, 3.0, 150, 14);
        spawnParticleArc(player, Particle.DUST, WHITE, 1.8, 150, 9);
        spawnParticleArc(player, Particle.SWEEP_ATTACK, null, 2.6, 150, 5);
        playSound(player, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.0f, 1.0f);
        return true;
    }
}
