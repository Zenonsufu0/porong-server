package com.poro.empire.combat.skills.axe;

import com.poro.empire.combat.SkillContext;
import com.poro.empire.combat.skills.PluginWeaponSkill;
import com.poro.empire.combat.weapon.WeaponType;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public final class AxeUnyieldingSkill extends PluginWeaponSkill {
    // pt:axe_endure_stance — 황금 불굴 오라 (자기 강화)
    private static final Particle.DustOptions GOLD = new Particle.DustOptions(Color.fromRGB(255, 200, 70), 1.3f);

    public AxeUnyieldingSkill(Plugin plugin) {
        super(plugin, "axe:unyielding", "불굴자세", WeaponType.AXE, 12000L);
    }

    @Override
    public boolean execute(Player player, SkillContext ctx) {
        // 5초 피해 감소 (RESISTANCE IV)
        player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 100, 3, false, false, true));

        // 황금 방어 오라 고리 + 강화음
        spawnParticleCircle(player, Particle.DUST, GOLD, 1.3, 22);
        spawnParticleCircle(player, Particle.ENCHANTED_HIT, null, 1.0, 10);
        playSound(player, Sound.ITEM_ARMOR_EQUIP_NETHERITE, 1.0f, 0.9f);
        playSound(player, Sound.BLOCK_ANVIL_LAND, 0.4f, 1.4f);
        return true;
    }
}
