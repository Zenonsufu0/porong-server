package com.poro.rpg.combat;

import com.poro.rpg.combat.weapon.WeaponType;
import com.poro.rpg.combat.weapon.WeaponTypeResolver;
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
    private com.poro.rpg.admin.AdminTogglesService toggles;   // 테스트용 쿨 우회 토글(나중 주입)

    public SkillService(SkillContext context) {
        this.context = context;
    }

    /** 관리자 토글 서비스 주입 — NO_SKILL_COOLDOWN ON 시 쿨다운 우회(테스트). */
    public void attachToggles(com.poro.rpg.admin.AdminTogglesService toggles) {
        this.toggles = toggles;
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

        boolean noCooldown = toggles != null
                && toggles.isOn(com.poro.rpg.admin.AdminTogglesService.Toggle.NO_SKILL_COOLDOWN);

        if (!noCooldown) {
            long remaining = context.getCooldownManager().getRemainingMillis(player.getUniqueId(), skill.key());
            if (remaining > 0) {
                return false; // 쿨다운 중 — HUD가 쿨타임 표시, 바닐라 공격 허용
            }
        }

        // 재진입 방지: execute() → dealDamage() → EntityDamageByEntityEvent 재발화 시
        // 쿨다운 체크에서 차단된다. execute() 실패 시 cancelCooldown()으로 롤백.
        // (NO_SKILL_COOLDOWN ON 시 쿨 미적용 — 단 재진입 방지 위해 0틱이라도 적용 후 즉시 해제)
        if (!noCooldown) {
            // 잠재 쿨타임 감소% 적용 (DL-129 2단계). applyCooldown이 totalMs를 저장하므로 HUD 진행바도 감소분 반영.
            double cdr = context.cooldownReductionPercent(player) / 100.0d;
            long effectiveCd = Math.max(0L, Math.round(skill.cooldown() * (1.0d - cdr)));
            context.getCooldownManager().applyCooldown(player.getUniqueId(), skill.key(), Duration.ofMillis(effectiveCd));
        }
        boolean used = skill.execute(player, context);
        if (!used) {
            if (!noCooldown) context.getCooldownManager().cancelCooldown(player.getUniqueId(), skill.key());
            return false;
        }
        return true; // 스킬 실제 발동 — 바닐라 공격 취소
    }

    public Collection<String> getSkillKeys() {
        return skillsByKey.keySet();
    }

    public WeaponSkill getSkill(String key) {
        return key == null ? null : skillsByKey.get(key.toLowerCase(Locale.ROOT));
    }
}
