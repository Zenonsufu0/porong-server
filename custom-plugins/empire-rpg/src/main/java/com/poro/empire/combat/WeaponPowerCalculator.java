package com.poro.empire.combat;

import com.poro.empire.common.registry.master.ItemMasterRegistry;
import com.poro.empire.common.registry.master.model.ItemMaster;
import com.poro.empire.growth.engine.EquipmentSlot;
import com.poro.empire.growth.engine.PlayerEquipmentItem;
import com.poro.empire.growth.engine.PlayerGrowthState;
import com.poro.empire.growth.engine.PotentialProfile;
import com.poro.empire.growth.engine.PotentialService;

public final class WeaponPowerCalculator {
    private static final double DEFAULT_POWER = 80.0d;

    /**
     * 강화 단계별 무기 ATK 누적 가산 (combat_balance_v2 §1, flat 비선형 / DL-091).
     * 인덱스 = 강화 단계(0~25). 총 ATK = 기본 ATK + ENHANCE_ATK_BONUS[level].
     * 마일스톤 점프: 10강 +10 / 18강 +20 / 25강 +15. (base 80 기준 0강 80 → 25강 192)
     */
    private static final int[] ENHANCE_ATK_BONUS = {
            0, 1, 2, 3, 4, 5, 7, 9, 11, 13,      // 0~9강
            23, 25, 27, 30, 33, 36, 40, 44,      // 10~17강 (10강 마일스톤)
            64, 69, 77, 82, 87, 92, 97, 112      // 18~25강 (18·25강 마일스톤)
    };

    private WeaponPowerCalculator() {
    }

    /** 강화 단계 → ATK 누적 가산. 범위 밖은 클램프. */
    public static int enhanceAtkBonus(int level) {
        if (level <= 0) return 0;
        return ENHANCE_ATK_BONUS[Math.min(level, ENHANCE_ATK_BONUS.length - 1)];
    }

    public static double calculate(
            PlayerGrowthState state,
            ItemMasterRegistry itemMasterRegistry,
            PotentialService potentialService
    ) {
        if (state == null || itemMasterRegistry == null) {
            return DEFAULT_POWER;
        }

        PlayerEquipmentItem equipped = state.equippedItem(EquipmentSlot.WEAPON).orElse(null);
        if (equipped == null) {
            return DEFAULT_POWER;
        }

        ItemMaster master = itemMasterRegistry.find(equipped.itemId()).orElse(null);
        if (master == null) {
            return DEFAULT_POWER;
        }

        double baseAtk = master.baseStatValue() > 0 ? master.baseStatValue() : DEFAULT_POWER;
        // flat 비선형 강화 가산 (CANON §1) — 선형 ×(1+0.1L) 대체 (DL-091)
        double enhancedAtk = baseAtk + enhanceAtkBonus(equipped.enhanceLevel());
        double potentialBonus = potentialBonus(equipped.potentialProfile());
        return enhancedAtk + potentialBonus;
    }

    private static double potentialBonus(PotentialProfile profile) {
        if (profile == null) {
            return 0.0d;
        }
        return profile.lines().stream().mapToDouble(line -> Math.max(0.0d, line.value())).sum();
    }
}
