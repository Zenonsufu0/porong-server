package kr.zenon.rpg.combat.skills.scythe;

import kr.zenon.rpg.combat.SkillContext;
import kr.zenon.rpg.combat.hitbox.SkillHitboxHelper;
import kr.zenon.rpg.combat.skills.BaseWeaponSkill;
import kr.zenon.rpg.combat.weapon.WeaponType;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;

public final class ScytheExecutionSkill extends BaseWeaponSkill {
    // pt:scythe_execute_slash — 처형, 보라 직선 + 영혼불
    private static final Particle.DustOptions PURPLE = new Particle.DustOptions(Color.fromRGB(160, 0, 200), 1.3f);

    public ScytheExecutionSkill() {
        // DL-129 추가#27: F 회전율 개선 — 쿨 16s→10s, 계수 하향(2.80→1.80 / 처형 4.80→3.10)로 DPS 중립 유지.
        // LC 스택 적립 주기(~10s에 3스택)와 맞춰 "3스택 차면 바로 F" 답답함 해소.
        super("scythe:execution", "처형낫", WeaponType.SCYTHE, 10000L);
    }

    @Override
    public boolean execute(Player player, SkillContext ctx) {
        consumeStacks(ctx, player);
        // 2D 이펙트: 전방 2.5블록 처형 표식 (바닥 평면, 발 높이)
        ctx.effectDisplay().spawnDecal(400108, player.getLocation(), 7.5f, 12);   // 처형 표식 (발 밑, 크게)
        SkillHitboxHelper.line(player, 4.0, 1.0).forEach(t -> {
            var maxHpAttr = t.getAttribute(Attribute.MAX_HEALTH);
            double hpRatio = (maxHpAttr != null && maxHpAttr.getValue() > 0)
                    ? t.getHealth() / maxHpAttr.getValue() : 1.0;
            double coeff = hpRatio < 0.30 ? 3.10 : 1.80;
            dealDamage(ctx, player, t, scaledDamage(ctx, player, coeff));
        });

        // 처형 직선 + 영혼불 + sweep + 음산한 처형음
        spawnParticleLine(player, Particle.DUST, PURPLE, 4.0, 24);
        spawnParticleLine(player, Particle.SOUL_FIRE_FLAME, null, 4.0, 14);
        spawnParticleArc(player, Particle.SWEEP_ATTACK, null, 3.0, 120, 4);
        playSound(player, Sound.ENTITY_WITHER_HURT, 0.8f, 0.7f);
        playSound(player, Sound.ENTITY_PLAYER_ATTACK_CRIT, 1.0f, 0.6f);
        return true;
    }
}
