package kr.zenon.rpg.combat.skills.scythe;

import kr.zenon.rpg.combat.SkillContext;
import kr.zenon.rpg.combat.hitbox.SkillHitboxHelper;
import kr.zenon.rpg.combat.skills.BaseWeaponSkill;
import kr.zenon.rpg.combat.weapon.WeaponType;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

public final class ScytheDeathSlashSkill extends BaseWeaponSkill {
    private static final Particle.DustOptions PURPLE = new Particle.DustOptions(Color.fromRGB(160, 0, 200), 1.2f);
    private static final Particle.DustOptions CRIMSON = new Particle.DustOptions(Color.fromRGB(180, 0, 60), 1.0f);

    public ScytheDeathSlashSkill() {
        super("scythe:death_slash", "사신베기", WeaponType.SCYTHE, 3000L);
    }

    @Override
    public boolean execute(Player player, SkillContext ctx) {
        double damage = scaledDamageWithStacks(ctx, player, 1.90, 0.05);
        var targets = SkillHitboxHelper.arc(player, 3.0, 150);
        targets.forEach(t -> dealDamage(ctx, player, t, damage));
        ctx.effectDisplay().spawnGroundTravel(400107, player, 6.0, 4.0f, 8, 0.6);   // 사신베기 (바닥 비행)

        // 사신베기(LC) 명중 시 1스택 충전 (DL-129 추가#25 — 월영회전 콤보 게이트 제거, LC 직접 적립).
        if (!targets.isEmpty()) {
            gainStack(ctx, player, 3);
        }

        // 보라 외호 + 검붉은 내호 + 바닐라 sweep 마크
        spawnParticleArc(player, Particle.DUST, PURPLE, 2.5, 150, 12);
        spawnParticleArc(player, Particle.DUST, CRIMSON, 1.5, 150,  8);
        spawnParticleArc(player, Particle.SWEEP_ATTACK, null, 2.0, 150, 5);
        playSound(player, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.0f, 0.75f);
        return true;
    }
}
