package kr.zenon.rpg.combat.skills.crossbow;

import kr.zenon.rpg.combat.SkillContext;
import kr.zenon.rpg.combat.hitbox.SkillHitboxHelper;
import kr.zenon.rpg.combat.skills.PluginWeaponSkill;
import kr.zenon.rpg.combat.weapon.WeaponType;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public final class CrossbowRapidFireSkill extends PluginWeaponSkill {
    // pt:crossbow_rapid_fire — 녹색 속사 볼트
    private static final Particle.DustOptions BOLT = new Particle.DustOptions(Color.fromRGB(120, 255, 130), 0.9f);

    public CrossbowRapidFireSkill(Plugin plugin) {
        super(plugin, "crossbow:rapid_fire", "속사", WeaponType.CROSSBOW, 3000L);
    }

    @Override
    public boolean execute(Player player, SkillContext ctx) {
        double damage = scaledDamage(ctx, player, 0.65);
        boolean[] hit = {false};
        for (int i = 0; i < 3; i++) {
            SkillHitboxHelper.projectileRaycast(player, 20.0, 0.5)
                    .ifPresent(t -> { dealDamage(ctx, player, t, damage); hit[0] = true; });
        }
        if (hit[0]) gainStack(ctx, player, 3);

        // 시선 녹색 빔 + 발사음 (속사 느낌으로 고음)
        spawnBeam(player, Particle.DUST, BOLT, 20.0, 0.5);
        spawnBeam(player, Particle.CRIT, null, 20.0, 1.0);
        playSound(player, Sound.ITEM_CROSSBOW_SHOOT, 1.0f, 1.5f);
        return true;
    }
}
