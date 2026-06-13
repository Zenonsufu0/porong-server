package kr.zenon.rpg.common.registry.master.model;

public record QuestMaster(
        String questId,
        String questType,
        String questName,
        String regionCode,
        String giverNpcId,
        String unlockConditionType,
        String unlockConditionValue,
        String objectiveType,
        String objectiveTargetId,
        int objectiveAmount,
        String rewardType,
        String rewardTargetId,
        int rewardAmount,
        String nextQuestId
) {
}
