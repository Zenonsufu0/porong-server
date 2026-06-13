package com.poro.rpg.common.registry.master.model;

public record AchievementMaster(
        String achievementId,
        String category,
        String achievementName,
        String conditionType,
        String conditionTargetId,
        int conditionAmount,
        String rewardType,
        String rewardTargetId,
        int rewardAmount,
        boolean hidden,
        boolean repeatable,
        String notes
) {
}
