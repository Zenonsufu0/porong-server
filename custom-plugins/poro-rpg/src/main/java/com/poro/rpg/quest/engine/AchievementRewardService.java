package com.poro.rpg.quest.engine;

import com.poro.rpg.common.registry.master.model.AchievementMaster;
import com.poro.rpg.common.result.ErrorCode;
import com.poro.rpg.common.result.Result;
import com.poro.rpg.common.time.TimeProvider;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public final class AchievementRewardService {
    private final HallOfFameHook hallOfFameHook;
    private final SymbolicHonorHook symbolicHonorHook;
    private final TimeProvider timeProvider;

    public AchievementRewardService(
            HallOfFameHook hallOfFameHook,
            SymbolicHonorHook symbolicHonorHook,
            TimeProvider timeProvider
    ) {
        this.hallOfFameHook = Objects.requireNonNull(hallOfFameHook, "hallOfFameHook");
        this.symbolicHonorHook = Objects.requireNonNull(symbolicHonorHook, "symbolicHonorHook");
        this.timeProvider = Objects.requireNonNull(timeProvider, "timeProvider");
    }

    public Result<RewardGrantResult> grantOnAchievementComplete(PlayerQuestHonorState state, AchievementMaster achievement) {
        if (state == null || achievement == null) {
            return Result.failure(ErrorCode.INVALID_ARGUMENT, "state and achievement are required.");
        }

        QuestRewardType rewardType;
        try {
            rewardType = QuestRewardType.from(achievement.rewardType());
        } catch (Exception exception) {
            return Result.failure(ErrorCode.INVALID_ARGUMENT, "Unknown achievement reward type: " + achievement.rewardType(), exception);
        }

        String targetId = normalize(achievement.rewardTargetId());
        long amount = Math.max(0, achievement.rewardAmount());
        if (amount <= 0L && rewardType != QuestRewardType.UNLOCK && rewardType != QuestRewardType.TITLE && rewardType != QuestRewardType.BADGE) {
            amount = 1L;
        }

        Map<String, String> metadata = new LinkedHashMap<>();
        switch (rewardType) {
            case GOLD -> state.addCurrency("gold", amount);
            case ITEM -> state.addItem(targetId, amount);
            case LIFE_EXP -> state.addLifeExp(targetId, amount);
            case UNLOCK -> state.unlockFlag(targetId);
            case TITLE -> {
                state.unlockTitle(targetId);
                metadata.put("title_unlocked", "true");
            }
            case BADGE -> {
                state.unlockBadge(targetId);
                metadata.put("badge_unlocked", "true");
            }
            case RECORD -> {
                state.addRecordStat(targetId, amount);
                hallOfFameHook.onAchievementRecorded(state, achievement, targetId, amount);
                symbolicHonorHook.onSymbolicHonorGranted(state, achievement, targetId, amount);
                metadata.put("hall_of_fame_hook", "called");
                metadata.put("symbolic_honor_hook", "called");
            }
        }

        return Result.success(new RewardGrantResult(
                achievement.achievementId(),
                rewardType,
                targetId,
                amount,
                timeProvider.nowInstant(),
                Map.copyOf(metadata)
        ));
    }

    public Result<Void> equipTitle(PlayerQuestHonorState state, String titleId) {
        if (state == null) {
            return Result.failure(ErrorCode.INVALID_ARGUMENT, "state is required.");
        }
        String normalized = normalize(titleId);
        if (!state.hasTitle(normalized)) {
            return Result.failure(ErrorCode.INVALID_ARGUMENT, "Title is not unlocked: " + titleId);
        }
        state.setEquippedTitleId(normalized);
        return Result.success();
    }

    public Result<Void> equipBadge(PlayerQuestHonorState state, String badgeId) {
        if (state == null) {
            return Result.failure(ErrorCode.INVALID_ARGUMENT, "state is required.");
        }
        String normalized = normalize(badgeId);
        if (!state.hasBadge(normalized)) {
            return Result.failure(ErrorCode.INVALID_ARGUMENT, "Badge is not unlocked: " + badgeId);
        }
        state.setEquippedBadgeId(normalized);
        return Result.success();
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    public record RewardGrantResult(
            String achievementId,
            QuestRewardType rewardType,
            String rewardTargetId,
            long rewardAmount,
            Instant grantedAt,
            Map<String, String> metadata
    ) {
    }
}
