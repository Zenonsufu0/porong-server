package kr.zenon.rpg.combat.skills.staff;

import kr.zenon.rpg.combat.SkillContext;
import kr.zenon.rpg.combat.hitbox.SkillHitboxHelper;
import kr.zenon.rpg.combat.skills.PluginWeaponSkill;
import kr.zenon.rpg.combat.weapon.WeaponType;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public final class StaffStarburstSkill extends PluginWeaponSkill {
    // pt:starlight_beam — 별빛 쇄도 빔
    private static final Particle.DustOptions STAR = new Particle.DustOptions(Color.fromRGB(190, 160, 255), 1.2f);

    public StaffStarburstSkill(Plugin plugin) {
        // DL-129 추가#28: F 회전율 정렬 — 쿨 20s→11s(3스택 적립 ~9s, 원거리 프리미엄 +1s), 계수 ×0.55 DPS 중립.
        super(plugin, "staff:starburst", "별빛쇄도", WeaponType.STAFF, 11000L);
    }

    @Override
    public boolean execute(Player player, SkillContext ctx) {
        double damage = scaledDamageFullChargeSpike(ctx, player, 2.14, 0.03, 1.20);
        SkillHitboxHelper.projectileRaycast(player, 22.0, 0.5)
                .ifPresent(t -> dealDamage(ctx, player, t, damage));
        consumeStacks(ctx, player);
        ctx.effectDisplay().spawnSlash(400104, player, 12.0, 5.0f, 9);   // 2D 이펙트 (별빛쇄도 빔)

        // 별빛 빔 (연보라 + 흰 별가루) + 별빛 시전음
        spawnBeam(player, Particle.DUST, STAR, 22.0, 0.4);
        spawnBeam(player, Particle.END_ROD, null, 22.0, 0.9);
        playSound(player, Sound.ENTITY_EVOKER_CAST_SPELL, 1.0f, 1.2f);
        playSound(player, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 1.5f);
        return true;
    }
}
