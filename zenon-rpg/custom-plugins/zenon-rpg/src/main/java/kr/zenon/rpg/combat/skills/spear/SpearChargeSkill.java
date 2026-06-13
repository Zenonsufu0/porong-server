package kr.zenon.rpg.combat.skills.spear;

import kr.zenon.rpg.combat.SkillContext;
import kr.zenon.rpg.combat.skills.PluginWeaponSkill;
import kr.zenon.rpg.combat.weapon.WeaponType;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public final class SpearChargeSkill extends PluginWeaponSkill {
    // pt:spear_dash_trail — 돌파 궤적 (청록 직선 + 구름 잔류)
    private static final Particle.DustOptions TEAL = new Particle.DustOptions(Color.fromRGB(90, 220, 210), 1.1f);

    public SpearChargeSkill(Plugin plugin) {
        super(plugin, "spear:charge", "돌파창", WeaponType.SPEAR, 9000L);
    }

    @Override
    public boolean execute(Player player, SkillContext ctx) {
        final double damage = scaledDamage(ctx, player, 2.60);
        // 시전 시 궤적 미리보기 + 추진음
        spawnParticleLine(player, Particle.DUST, TEAL, 5.0, 26);
        playSound(player, Sound.ENTITY_PLAYER_ATTACK_KNOCKBACK, 1.0f, 0.9f);
        // 9틱 전방 돌진(~5.4블록) — 경로 전체 적 1회씩 타격 + 돌진 중 무적
        dashStrike(player, player.getLocation().getDirection(), 9, 0.6, 2.2, true, TEAL,
                e -> dealDamage(ctx, player, e, damage), null);
        return true;
    }
}
