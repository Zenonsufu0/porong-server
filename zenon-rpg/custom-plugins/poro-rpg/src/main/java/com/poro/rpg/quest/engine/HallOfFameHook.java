package com.poro.rpg.quest.engine;

import com.poro.rpg.common.registry.master.model.AchievementMaster;

public interface HallOfFameHook {
    void onAchievementRecorded(PlayerQuestHonorState state, AchievementMaster achievement, String recordId, long amount);
}
