package kr.zenon.rpg.combat.skills.axe;

import kr.zenon.rpg.combat.SkillContext;
import kr.zenon.rpg.combat.skills.PluginWeaponSkill;
import kr.zenon.rpg.combat.weapon.WeaponType;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public final class AxeCrushChargeSkill extends PluginWeaponSkill {
    // pt:axe_crush_charge — 파쇄 돌진 궤적
    private static final Particle.DustOptions AMBER = new Particle.DustOptions(Color.fromRGB(220, 140, 40), 1.3f);

    public AxeCrushChargeSkill(Plugin plugin) {
        super(plugin, "axe:crush_charge", "파쇄돌진", WeaponType.AXE, 5000L);
    }

    @Override
    public boolean execute(Player player, SkillContext ctx) {
        final double damage = scaledDamage(ctx, player, 3.05);
        // 시전 시 궤적 미리보기 + 추진음
        spawnParticleLine(player, Particle.DUST, AMBER, 4.0, 22);
        playSound(player, Sound.ENTITY_PLAYER_ATTACK_KNOCKBACK, 1.0f, 0.8f);
        // 6틱 전방 돌진(~3.6블록) — 경로 전체 적 1회씩 타격 + 돌진 중 무적
        dashStrike(player, player.getLocation().getDirection(), 6, 0.6, 2.2, true, AMBER,
                e -> dealDamage(ctx, player, e, damage), null);
        return true;
    }
}
