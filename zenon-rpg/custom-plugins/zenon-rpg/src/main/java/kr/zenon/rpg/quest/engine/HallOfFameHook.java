package kr.zenon.rpg.quest.engine;

import kr.zenon.rpg.common.registry.master.model.AchievementMaster;

public interface HallOfFameHook {
    void onAchievementRecorded(PlayerQuestHonorState state, AchievementMaster achievement, String recordId, long amount);
}
