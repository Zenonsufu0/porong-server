package com.poro.rpg.quest.engine;

import com.poro.rpg.common.registry.master.model.AchievementMaster;

public final class NoopSymbolicHonorHook implements SymbolicHonorHook {
    @Override
    public void onSymbolicHonorGranted(PlayerQuestHonorState state, AchievementMaster achievement, String honorCode, long amount) {
        // no-op
    }
}
