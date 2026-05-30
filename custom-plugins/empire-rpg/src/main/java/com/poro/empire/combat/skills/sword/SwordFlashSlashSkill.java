package com.poro.empire.combat.skills.sword;

import com.poro.empire.combat.SkillContext;
import com.poro.empire.combat.hitbox.SkillHitboxHelper;
import com.poro.empire.combat.skills.BaseWeaponSkill;
import com.poro.empire.combat.weapon.WeaponType;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

public final class SwordFlashSlashSkill extends BaseWeaponSkill {
    // pt:sword_flash_slash — 흰빛 섬광 베기
    private static final Particle.DustOptions FLASH = new Particle.DustOptions(Color.fromRGB(225, 240, 255), 1.0f);

    public SwordFlashSlashSkill() {
        super("sword:flash_slash", "섬광베기", WeaponType.SWORD, 3000L);
    }

    @Override
    public boolean execute(Player player, SkillContext ctx) {
        dashForward(player, 2.0);
        double damage = scaledDamageWithStacks(ctx, player, 1.60, 0.08);
        SkillHitboxHelper.arc(player, 2.5, 120).forEach(t -> dealDamage(player, t, damage));
        gainStack(ctx, player, 3);

        // 흰빛 2겹 호 + sweep 마크 + 잔광(END_ROD)
        spawnParticleArc(player, Particle.DUST, FLASH, 2.5, 120, 12);
        spawnParticleArc(player, Particle.DUST, FLASH, 1.3, 120, 7);
        spawnParticleArc(player, Particle.SWEEP_ATTACK, null, 2.2, 120, 4);
        spawnParticleArc(player, Particle.END_ROD, null, 2.4, 120, 6);
        playSound(player, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.9f, 1.5f);
        return true;
    }
}
