package kr.zenon.rpg.combat.skills.scythe;

import kr.zenon.rpg.combat.SkillContext;
import kr.zenon.rpg.combat.skills.PluginWeaponSkill;
import kr.zenon.rpg.combat.weapon.WeaponType;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public final class ScytheShadowSpinSkill extends PluginWeaponSkill {
    private static final Particle.DustOptions VIOLET = new Particle.DustOptions(Color.fromRGB(100, 0, 180), 1.0f);

    public ScytheShadowSpinSkill(Plugin plugin) {
        super(plugin, "scythe:shadow_spin", "월영회전", WeaponType.SCYTHE, 5000L);
    }

    @Override
    public boolean execute(Player player, SkillContext ctx) {
        final double damage = scaledDamage(ctx, player, 0.60);
        playSound(player, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.0f, 1.4f);
        playSound(player, Sound.ENTITY_ENDERMAN_TELEPORT, 0.6f, 1.6f);
        // 8틱(0.4s) 전방 돌진 — 경로 전체 적 1회씩 타격 + 돌진 중 무적
        dashStrike(player, player.getLocation().getDirection(), 8, 0.7, 2.5, true, VIOLET,
                e -> dealDamage(ctx, player, e, damage), null);
        return true;
    }
}
