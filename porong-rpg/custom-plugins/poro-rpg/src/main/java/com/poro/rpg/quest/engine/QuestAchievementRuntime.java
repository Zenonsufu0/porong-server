package com.poro.rpg.quest.engine;

public record QuestAchievementRuntime(
        QuestStateService questStateService,
        QuestObjectiveEvaluator objectiveEvaluator,
        QuestRewardResolver rewardResolver,
        QuestChainService questChainService,
        AchievementTracker achievementTracker,
        AchievementRewardService achievementRewardService,
        PlayerQuestAndHonorSnapshotBuilder snapshotBuilder,
        QuestRewardRegistry questRewardRegistry,
        QuestChainBranchRegistry questChainBranchRegistry,
        InMemoryQuestRecordRewardHook questRecordRewardHook,
        InMemoryHallOfFameHook hallOfFameHook,
        InMemorySymbolicHonorHook symbolicHonorHook
) {
}
