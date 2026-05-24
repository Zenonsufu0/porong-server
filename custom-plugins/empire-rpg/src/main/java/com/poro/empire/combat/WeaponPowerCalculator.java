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

    private WeaponPowerCalculator() {
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
        double enhanceBonus = 1.0d + (equipped.enhanceLevel() * 0.1d);
        double potentialBonus = potentialBonus(equipped.potentialProfile());
        return baseAtk * enhanceBonus + potentialBonus;
    }

    private static double potentialBonus(PotentialProfile profile) {
        if (profile == null) {
            return 0.0d;
        }
        return profile.lines().stream().mapToDouble(line -> Math.max(0.0d, line.value())).sum();
    }
}
