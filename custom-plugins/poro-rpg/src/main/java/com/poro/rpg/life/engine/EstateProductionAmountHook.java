package com.poro.rpg.life.engine;

public interface EstateProductionAmountHook {
    int adjustBaseMin(PlayerLifeState state, EstateFacilityMaster facility, EstateFacilityLevelRule levelRule, int baseMin);

    int adjustBaseMax(PlayerLifeState state, EstateFacilityMaster facility, EstateFacilityLevelRule levelRule, int baseMax);

    double adjustRareChancePercent(
            PlayerLifeState state,
            EstateFacilityMaster facility,
            EstateFacilityLevelRule levelRule,
            double rareChancePercent
    );
}
