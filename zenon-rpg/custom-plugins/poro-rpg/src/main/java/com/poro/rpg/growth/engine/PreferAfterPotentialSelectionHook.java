package com.poro.rpg.growth.engine;

public final class PreferAfterPotentialSelectionHook implements PotentialSelectionHook {
    @Override
    public PotentialProfile choose(
            String operationType,
            PlayerGrowthState state,
            PlayerEquipmentItem item,
            PotentialProfile before,
            PotentialProfile after
    ) {
        return after;
    }
}
