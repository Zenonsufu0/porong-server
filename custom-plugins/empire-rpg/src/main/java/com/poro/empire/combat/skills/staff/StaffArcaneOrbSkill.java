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

public final class StaffArcaneOrbSkill extends PluginWeaponSkill {
    // pt:magic_bolt — 비전 보라 마력탄
    private static final Particle.DustOptions ARCANE = new Particle.DustOptions(Color.fromRGB(160, 110, 255), 1.1f);

    public StaffArcaneOrbSkill(Plugin plugin) {
        super(plugin, "staff:arcane_orb", "마력탄", WeaponType.STAFF, 3000L);
    }

    @Override
    public boolean execute(Player player, SkillContext ctx) {
        double damage = scaledDamage(ctx, player, 1.70);
        SkillHitboxHelper.projectileRaycast(player, 20.0, 0.5)
                .ifPresent(t -> { dealDamage(ctx, player, t, damage); gainStack(ctx, player, 3); });   // 명중 시에만 충전 (정본 §4)

        // 보라 마력 빔 + 마법 입자 + 시전음
        spawnBeam(player, Particle.DUST, ARCANE, 20.0, 0.5);
        spawnBeam(player, Particle.WITCH, null, 20.0, 1.2);
        playSound(player, Sound.ENTITY_EVOKER_CAST_SPELL, 0.8f, 1.4f);
        return true;
    }
}
