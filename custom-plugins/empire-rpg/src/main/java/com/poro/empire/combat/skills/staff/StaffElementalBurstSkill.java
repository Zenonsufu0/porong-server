package com.poro.empire.combat.skills.staff;

import com.poro.empire.combat.SkillContext;
import com.poro.empire.combat.hitbox.SkillHitboxHelper;
import com.poro.empire.combat.skills.PluginWeaponSkill;
import com.poro.empire.combat.weapon.WeaponType;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public final class StaffElementalBurstSkill extends PluginWeaponSkill {
    // pt:staff_element_burst — 비전 빔 + 착탄 폭발
    private static final Particle.DustOptions ARCANE = new Particle.DustOptions(Color.fromRGB(160, 110, 255), 1.2f);

    public StaffElementalBurstSkill(Plugin plugin) {
        super(plugin, "staff:elemental_burst", "속성폭발", WeaponType.STAFF, 6000L);
    }

    @Override
    public boolean execute(Player player, SkillContext ctx) {
        double damage = scaledDamage(ctx, player, 2.40);
        SkillHitboxHelper.projectileRaycast(player, 18.0, 0.5).ifPresent(primary -> {
            dealDamage(player, primary, damage);
            // AoE burst at impact location
            primary.getWorld().getNearbyLivingEntities(primary.getLocation(), 2.5).stream()
                    .filter(e -> !e.equals(player) && !e.equals(primary))
                    .forEach((LivingEntity splash) -> dealDamage(player, splash, damage));
            // 착탄 지점 폭발 연출
            spawnImpactEffect(primary.getLocation().add(0, 1.0, 0), Particle.DUST, ARCANE, 30);
            spawnImpactEffect(primary.getLocation().add(0, 1.0, 0), Particle.WITCH, null, 16);
        });
        gainStack(ctx, player, 5);

        // 비전 빔 + 시전음
        spawnBeam(player, Particle.DUST, ARCANE, 18.0, 0.5);
        playSound(player, Sound.ENTITY_EVOKER_CAST_SPELL, 1.0f, 1.0f);
        return true;
    }
}
