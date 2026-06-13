package kr.zenon.rpg.combat.skills.axe;

import kr.zenon.rpg.combat.SkillContext;
import kr.zenon.rpg.combat.hitbox.SkillHitboxHelper;
import kr.zenon.rpg.combat.skills.BaseWeaponSkill;
import kr.zenon.rpg.combat.weapon.WeaponType;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

public final class AxeColossalDropSkill extends BaseWeaponSkill {
    // pt:axe_impact_burst — 거신 추락, 지면 충격파
    private static final Particle.DustOptions AMBER = new Particle.DustOptions(Color.fromRGB(220, 140, 40), 1.4f);

    public AxeColossalDropSkill() {
        // DL-129 추가#28: F 회전율 정렬 — 쿨 18s→13s(LC 4s, 3스택 적립 ~12s), 계수 ×0.722 DPS 중립.
        super("axe:colossal_drop", "거신추락", WeaponType.AXE, 13000L);
    }

    @Override
    public boolean execute(Player player, SkillContext ctx) {
        double damage = scaledDamageWithStacks(ctx, player, 3.29, 0.07);
        SkillHitboxHelper.burst(player, 4.5).forEach(t -> dealDamage(ctx, player, t, damage));
        consumeStacks(ctx, player);
        // 2D 이펙트: 착탄 균열(바닥 장판) + 공용 임팩트링(충격파 고리). 링은 균열을 감싸도록 크게·짧게 팝.
        // 흰 코어 과다 보류 해소(2026-06-02): 중앙 투명화로 고리 형태 정상 — 두 평면 z-fight 방지로 링만 0.02블록 띄움.
        ctx.effectDisplay().spawnDecal(400106, player.getLocation(), 11.0f, 14);
        ctx.effectDisplay().spawnDecal(400901, player.getLocation().add(0, 0.02, 0), 13.0f, 10);

        // 지면 충격파 2중 고리 + 중앙 폭발 + 거대 타격음
        spawnParticleCircle(player, Particle.DUST, AMBER, 4.5, 40);
        spawnParticleCircle(player, Particle.DUST, AMBER, 2.8, 26);
        spawnImpactEffect(player.getLocation(), Particle.EXPLOSION, null, 1);
        spawnImpactEffect(player.getLocation().add(0, 0.5, 0), Particle.CRIT, null, 24);
        playSound(player, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 0.7f);
        playSound(player, Sound.ENTITY_IRON_GOLEM_ATTACK, 1.0f, 0.7f);
        return true;
    }
}
