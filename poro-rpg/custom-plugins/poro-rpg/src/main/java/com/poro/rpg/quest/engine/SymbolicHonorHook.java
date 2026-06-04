package com.poro.rpg.quest.engine;

import com.poro.rpg.common.registry.master.model.AchievementMaster;

public interface SymbolicHonorHook {
    void onSymbolicHonorGranted(PlayerQuestHonorState state, AchievementMaster achievement, String honorCode, long amount);
}
