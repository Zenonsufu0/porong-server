package kr.zenon.rpg.quest.engine;

import kr.zenon.rpg.common.registry.master.model.AchievementMaster;

public interface SymbolicHonorHook {
    void onSymbolicHonorGranted(PlayerQuestHonorState state, AchievementMaster achievement, String honorCode, long amount);
}
