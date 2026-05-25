package com.poro.empire.combat.skills.axe;

import com.poro.empire.combat.SkillContext;
import com.poro.empire.combat.skills.PluginWeaponSkill;
import com.poro.empire.combat.weapon.WeaponType;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public final class AxeUnyieldingSkill extends PluginWeaponSkill {
    public AxeUnyieldingSkill(Plugin plugin) {
        super(plugin, "axe:unyielding", "불굴자세", WeaponType.AXE, 12000L);
    }

    @Override
    public boolean execute(Player player, SkillContext ctx) {
        // 5초 피해 감소 (RESISTANCE IV)
        player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 100, 3, false, false, true));
        return true;
    }
}
