package com.poro.empire.combat.skills.staff;

import com.poro.empire.combat.SkillContext;
import com.poro.empire.combat.hitbox.SkillHitboxHelper;
import com.poro.empire.combat.skills.PluginWeaponSkill;
import com.poro.empire.combat.weapon.WeaponType;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public final class StaffStarburstSkill extends PluginWeaponSkill {
    // pt:starlight_beam — 별빛 쇄도 빔
    private static final Particle.DustOptions STAR = new Particle.DustOptions(Color.fromRGB(190, 160, 255), 1.2f);

    public StaffStarburstSkill(Plugin plugin) {
        super(plugin, "staff:starburst", "별빛쇄도", WeaponType.STAFF, 20000L);
    }

    @Override
    public boolean execute(Player player, SkillContext ctx) {
        double damage = scaledDamageWithStacks(ctx, player, 4.05, 0.10);
        SkillHitboxHelper.projectileRaycast(player, 22.0, 0.5)
                .ifPresent(t -> dealDamage(ctx, player, t, damage));
        consumeStacks(ctx, player);

        // 별빛 빔 (연보라 + 흰 별가루) + 별빛 시전음
        spawnBeam(player, Particle.DUST, STAR, 22.0, 0.4);
        spawnBeam(player, Particle.END_ROD, null, 22.0, 0.9);
        playSound(player, Sound.ENTITY_EVOKER_CAST_SPELL, 1.0f, 1.2f);
        playSound(player, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 1.5f);
        return true;
    }
}
