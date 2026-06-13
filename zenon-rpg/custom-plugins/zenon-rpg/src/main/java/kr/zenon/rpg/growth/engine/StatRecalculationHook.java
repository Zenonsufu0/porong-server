package kr.zenon.rpg.growth.engine;

public interface StatRecalculationHook {
    void onRecalculate(PlayerGrowthState state);
}
