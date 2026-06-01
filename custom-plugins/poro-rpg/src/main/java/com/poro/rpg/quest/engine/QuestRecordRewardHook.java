package com.poro.rpg.quest.engine;

public interface QuestRecordRewardHook {
    void onQuestRecordReward(PlayerQuestHonorState state, String sourceId, String recordId, long amount);
}
