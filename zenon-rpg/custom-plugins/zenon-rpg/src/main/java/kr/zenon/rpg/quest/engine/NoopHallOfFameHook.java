package kr.zenon.rpg.quest.engine;

import kr.zenon.rpg.common.registry.master.model.AchievementMaster;

public final class NoopHallOfFameHook implements HallOfFameHook {
    @Override
    public void onAchievementRecorded(PlayerQuestHonorState state, AchievementMaster achievement, String recordId, long amount) {
        // no-op
    }
}
