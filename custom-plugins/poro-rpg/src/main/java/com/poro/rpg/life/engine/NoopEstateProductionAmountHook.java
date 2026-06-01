package com.poro.rpg.life.engine;

public final class NoopEstateProductionAmountHook implements EstateProductionAmountHook {
    @Override
    public int adjustBaseMin(PlayerLifeState state, EstateFacilityMaster facility, EstateFacilityLevelRule levelRule, int baseMin) {
        return baseMin;
    }

    @Override
    public int adjustBaseMax(PlayerLifeState state, EstateFacilityMaster facility, EstateFacilityLevelRule levelRule, int baseMax) {
        return baseMax;
    }

    @Override
    public double adjustRareChancePercent(
            PlayerLifeState state,
            EstateFacilityMaster facility,
            EstateFacilityLevelRule levelRule,
            double rareChancePercent
    ) {
        return rareChancePercent;
    }
}
