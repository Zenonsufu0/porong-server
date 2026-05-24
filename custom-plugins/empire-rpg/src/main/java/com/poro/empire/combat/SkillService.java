package com.poro.empire.combat;

import com.poro.empire.combat.weapon.WeaponType;
import com.poro.empire.combat.weapon.WeaponTypeResolver;
import net.kyori.adventure.text.Component;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public class SkillService {
    private final Map<String, WeaponSkill> skillsByKey = new LinkedHashMap<>();
    private final SkillContext context;

    public SkillService(SkillContext context) {
        this.context = context;
    }

    public void registerSkill(WeaponSkill skill) {
        skillsByKey.put(skill.key().toLowerCase(Locale.ROOT), skill);
    }

    public boolean useSkill(Player player, String rawSkillKey) {
        String skillKey = rawSkillKey.toLowerCase(Locale.ROOT);
        WeaponSkill skill = skillsByKey.get(skillKey);
        if (skill == null) {
            player.sendMessage(ChatColor.RED + "Unknown skill: " + rawSkillKey);
            return true;
        }

        WeaponType currentWeaponType = WeaponTypeResolver.resolve(player);
        if (currentWeaponType != skill.weaponType()) {
            player.sendMessage(ChatColor.RED + "Wrong weapon type. Required: "
                    + skill.weaponType().name().toLowerCase(Locale.ROOT)
                    + ", current: "
                    + currentWeaponType.name().toLowerCase(Locale.ROOT));
            return true;
        }

        long remaining = context.getCooldownManager().getRemainingMillis(player.getUniqueId(), skill.key());
        if (remaining > 0) {
            player.sendActionBar(Component.text(
                    "§e" + skill.displayName() + " §c" + CooldownManager.formatSeconds(remaining) + "s"));
            return true;
        }

        boolean used = skill.execute(player, context);
        if (!used) {
            return true;
        }

        context.getCooldownManager().applyCooldown(player.getUniqueId(), skill.key(), Duration.ofMillis(skill.cooldown()));
        return true;
    }

    public Collection<String> getSkillKeys() {
        return skillsByKey.keySet();
    }
}
