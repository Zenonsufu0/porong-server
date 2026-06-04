package com.poro.rpg.combat.skills.sword;

import com.poro.rpg.combat.SkillContext;
import com.poro.rpg.combat.skills.PluginWeaponSkill;
import com.poro.rpg.combat.weapon.WeaponType;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public final class SwordFlashSlashSkill extends PluginWeaponSkill {
    // pt:sword_flash_slash — 흰빛 섬광 베기
    private static final Particle.DustOptions FLASH = new Particle.DustOptions(Color.fromRGB(225, 240, 255), 1.0f);

    public SwordFlashSlashSkill(Plugin plugin) {
        super(plugin, "sword:flash_slash", "섬광베기", WeaponType.SWORD, 3000L);
    }

    @Override
    public boolean execute(Player player, SkillContext ctx) {
        // 돌진 경로 전체 타격으로 변경 — 붙어서 쳐도 지나치는 적까지 명중(기존 arc 빗나감 해소).
        final double damage = scaledDamageWithStacks(ctx, player, 1.70, 0.08);

        // 흰빛 호 + sweep + 잔광(시전 연출)
        spawnParticleArc(player, Particle.DUST, FLASH, 2.5, 120, 12);
        spawnParticleArc(player, Particle.SWEEP_ATTACK, null, 2.2, 120, 4);
        spawnParticleArc(player, Particle.END_ROD, null, 2.4, 120, 6);
        playSound(player, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.9f, 1.5f);

        // 5틱 전방 돌진(~2.75블록) — 경로 적 1회씩 타격 + 무적, 1명이라도 맞히면 1스택 충전(정본 §4)
        dashStrike(player, player.getLocation().getDirection(), 5, 0.55, 2.2, true, FLASH,
                e -> dealDamage(ctx, player, e, damage),
                count -> { if (count > 0) gainStack(ctx, player, 3); });
        return true;
    }
}
