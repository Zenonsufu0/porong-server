package com.poro.rpg.combat;

import com.poro.rpg.common.registry.master.ItemMasterRegistry;
import com.poro.rpg.common.registry.master.model.ItemMaster;
import com.poro.rpg.growth.engine.EquipmentSlot;
import com.poro.rpg.growth.engine.PlayerEquipmentItem;
import com.poro.rpg.growth.engine.PlayerGrowthState;
import com.poro.rpg.growth.engine.PotentialProfile;
import com.poro.rpg.growth.engine.PotentialService;

public final class WeaponPowerCalculator {
    private static final double DEFAULT_POWER = 80.0d;

    /**
     * 강화 단계별 무기 ATK 누적 가산 (DL-128#13 상향: 강화 체감 ~1.8배 — HP 산식 상향과 균형).
     * 인덱스 = 강화 단계(0~25). 총 ATK = 기본 ATK + ENHANCE_ATK_BONUS[level].
     * base 80 기준: 0강 80 / 10강 121 / 18강 195(2.4배) / 25강 282(3.5배). 마일스톤 점프 10·18·25강.
     */
    private static final int[] ENHANCE_ATK_BONUS = {
            0, 2, 4, 5, 7, 9, 13, 16, 20, 23,    // 0~9강
            41, 45, 49, 54, 59, 65, 72, 79,      // 10~17강 (10강 마일스톤)
            115, 124, 139, 148, 157, 166, 175, 202 // 18~25강 (18·25강 마일스톤)
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
        // 잠재 attack_percent만 ATK에 승산 적용 (DL-092). general/boss 피해증가는 ATK가 아니라
        // 피해 공식 단계에서 곱해지므로 여기서 합산하지 않는다.
        // 정본상 ATK%는 무기·신발 슬롯 모두 보유 → 장착 전 슬롯에서 합산(DL-129 3단계).
        double attackPercent = attackPercentEquipped(state);
        return enhancedAtk * (1.0d + attackPercent / 100.0d);
    }

    /** 장착 전 슬롯(무기+신발) 잠재의 attack_percent 합(%). */
    private static double attackPercentEquipped(PlayerGrowthState state) {
        double sum = 0.0d;
        for (String instanceId : state.equippedItems().values()) {
            PlayerEquipmentItem item = state.inventoryItem(instanceId).orElse(null);
            if (item == null) continue;
            sum += attackPercentOf(item.potentialProfile());
        }
        return sum;
    }

    /** 잠재 프로필의 attack_percent 합(%). */
    private static double attackPercentOf(PotentialProfile profile) {
        if (profile == null) return 0.0d;
        return profile.lines().stream()
                .filter(line -> "attack_percent".equals(line.optionCode()))
                .mapToDouble(line -> Math.max(0.0d, line.value()))
                .sum();
    }
}
