package com.poro.rpg.growth.engine;

public interface PotentialSelectionHook {
    PotentialProfile choose(
            String operationType,
            PlayerGrowthState state,
            PlayerEquipmentItem item,
            PotentialProfile before,
            PotentialProfile after
    );
}
