package com.poro.rpg.growth.engine;

public interface StatRecalculationHook {
    void onRecalculate(PlayerGrowthState state);
}
