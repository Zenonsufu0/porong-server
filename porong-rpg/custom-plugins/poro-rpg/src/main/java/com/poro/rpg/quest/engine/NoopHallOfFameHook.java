package com.poro.rpg.quest.engine;

import com.poro.rpg.common.registry.master.model.AchievementMaster;

public final class NoopHallOfFameHook implements HallOfFameHook {
    @Override
    public void onAchievementRecorded(PlayerQuestHonorState state, AchievementMaster achievement, String recordId, long amount) {
        // no-op
    }
}
