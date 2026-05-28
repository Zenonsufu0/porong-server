package com.poro.empire.combat;

import com.poro.empire.combat.weapon.WeaponType;
import com.poro.empire.combat.weapon.WeaponTypeResolver;
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
            return false; // 쿨다운 중 — HUD가 쿨타임 표시, 바닐라 공격 허용
        }

        // 재진입 방지: execute() → dealDamage() → EntityDamageByEntityEvent 재발화 시
        // 쿨다운 체크(line 43)에서 차단된다. execute() 실패 시 cancelCooldown()으로 롤백.
        context.getCooldownManager().applyCooldown(player.getUniqueId(), skill.key(), Duration.ofMillis(skill.cooldown()));
        boolean used = skill.execute(player, context);
        if (!used) {
            context.getCooldownManager().cancelCooldown(player.getUniqueId(), skill.key());
            return false;
        }
        return true; // 스킬 실제 발동 — 바닐라 공격 취소
    }

    public Collection<String> getSkillKeys() {
        return skillsByKey.keySet();
    }
}
