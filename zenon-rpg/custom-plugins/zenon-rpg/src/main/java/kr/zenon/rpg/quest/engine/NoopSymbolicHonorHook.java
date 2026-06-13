package kr.zenon.rpg.quest.engine;

import kr.zenon.rpg.common.registry.master.model.AchievementMaster;

public final class NoopSymbolicHonorHook implements SymbolicHonorHook {
    @Override
    public void onSymbolicHonorGranted(PlayerQuestHonorState state, AchievementMaster achievement, String honorCode, long amount) {
        // no-op
    }
}
