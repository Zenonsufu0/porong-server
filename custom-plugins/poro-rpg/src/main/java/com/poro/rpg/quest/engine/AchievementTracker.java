package com.poro.rpg.quest.engine;

import com.poro.rpg.common.registry.master.AchievementMasterRegistry;
import com.poro.rpg.common.registry.master.model.AchievementMaster;
import com.poro.rpg.common.result.ErrorCode;
import com.poro.rpg.common.result.Result;
import com.poro.rpg.common.time.TimeProvider;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public final class AchievementTracker {
    private final AchievementMasterRegistry achievementMasterRegistry;
    private final AchievementRewardService rewardService;
    private final TimeProvider timeProvider;

    public AchievementTracker(
            AchievementMasterRegistry achievementMasterRegistry,
            AchievementRewardService rewardService,
            TimeProvider timeProvider
    ) {
        this.achievementMasterRegistry = Objects.requireNonNull(achievementMasterRegistry, "achievementMasterRegistry");
        this.rewardService = Objects.requireNonNull(rewardService, "rewardService");
        this.timeProvider = Objects.requireNonNull(timeProvider, "timeProvider");
    }

    public Result<TrackingResult> onEvent(PlayerQuestHonorState state, QuestEvent event) {
        if (state == null || event == null) {
            return Result.failure(ErrorCode.INVALID_ARGUMENT, "state and event are required.");
        }

        List<String> completedAchievementIds = new ArrayList<>();
        List<AchievementRewardService.RewardGrantResult> rewardGrants = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        Instant now = timeProvider.nowInstant();

        for (AchievementMaster achievement : achievementMasterRegistry.all().values()) {
            String achievementId = normalize(achievement.achievementId());
            boolean alreadyCompleted = state.isAchievementCompleted(achievementId);
            if (alreadyCompleted && !achievement.repeatable()) {
                continue;
            }

            MatchResult matchResult = matchAndIncrement(achievement, event);
            if (!matchResult.matched()) {
                continue;
            }

            long current = state.achievementProgress(achievementId);
            long after = current + matchResult.increment();
            state.setAchievementProgress(achievementId, after);

            long required = Math.max(1L, achievement.conditionAmount());
            if (after < required) {
                continue;
            }

            state.markAchievementCompleted(achievementId, now);
            completedAchievementIds.add(achievementId);

            Result<AchievementRewardService.RewardGrantResult> rewardResult = rewardService.grantOnAchievementComplete(state, achievement);
            if (rewardResult.isFailure()) {
                warnings.add("Failed to grant achievement reward. achievement_id=" + achievementId + ", reason=" + rewardResult.message());
                continue;
            }
            rewardGrants.add(rewardResult.value());
        }

        if (!completedAchievementIds.isEmpty() && state.featuredAchievementIds().isEmpty()) {
            state.setFeaturedAchievementIds(completedAchievementIds);
        }

        return Result.success(new TrackingResult(
                event,
                List.copyOf(completedAchievementIds),
                List.copyOf(rewardGrants),
                List.copyOf(warnings)
        ));
    }

    private MatchResult matchAndIncrement(AchievementMaster achievement, QuestEvent event) {
        AchievementConditionType conditionType;
        try {
            conditionType = AchievementConditionType.from(achievement.conditionType());
        } catch (Exception ignored) {
            return new MatchResult(false, 0L);
        }

        boolean matched = switch (conditionType) {
            case BOSS_CLEAR -> (event.eventType() == ProgressEventType.BOSS_CLEAR
                    || event.eventType() == ProgressEventType.CLEAR_BOSS)
                    && targetMatches(achievement.conditionTargetId(), event.targetId());
            case NO_DEATH_CLEAR -> event.eventType() == ProgressEventType.NO_DEATH_CLEAR
                    && targetMatches(achievement.conditionTargetId(), event.targetId());
            case FIRST_CRAFT, CRAFT_ANY -> event.eventType() == ProgressEventType.CRAFT
                    && targetMatches(achievement.conditionTargetId(), event.targetId());
            case ESTATE_UNLOCK -> event.eventType() == ProgressEventType.ESTATE_UNLOCK
                    && targetMatches(achievement.conditionTargetId(), event.targetId());
            case REGION_DISCOVER -> (event.eventType() == ProgressEventType.REGION_DISCOVER
                    || event.eventType() == ProgressEventType.ENTER_REGION)
                    && targetMatches(achievement.conditionTargetId(), event.targetId());
            case EXTREME_CLEAR -> event.eventType() == ProgressEventType.EXTREME_CLEAR
                    && targetMatches(achievement.conditionTargetId(), event.targetId());
            case QUEST_CLEAR -> event.eventType() == ProgressEventType.QUEST_CLEAR
                    && targetMatches(achievement.conditionTargetId(), event.targetId());
        };
        if (!matched) {
            return new MatchResult(false, 0L);
        }

        long increment = Math.max(1L, event.amount());
        if (conditionType == AchievementConditionType.BOSS_CLEAR
                || conditionType == AchievementConditionType.NO_DEATH_CLEAR
                || conditionType == AchievementConditionType.ESTATE_UNLOCK
                || conditionType == AchievementConditionType.REGION_DISCOVER
                || conditionType == AchievementConditionType.EXTREME_CLEAR
                || conditionType == AchievementConditionType.QUEST_CLEAR) {
            increment = 1L;
        }
        return new MatchResult(true, increment);
    }

    private boolean targetMatches(String expected, String actual) {
        String normalizedExpected = normalize(expected);
        if (normalizedExpected.isBlank() || "any".equals(normalizedExpected) || "-".equals(normalizedExpected) || "none".equals(normalizedExpected)) {
            return true;
        }
        return normalizedExpected.equals(normalize(actual));
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private record MatchResult(
            boolean matched,
            long increment
    ) {
    }

    public record TrackingResult(
            QuestEvent event,
            List<String> completedAchievementIds,
            List<AchievementRewardService.RewardGrantResult> rewardGrants,
            List<String> warnings
    ) {
        public Map<String, String> completionSummary() {
            Map<String, String> summary = new LinkedHashMap<>();
            for (String achievementId : completedAchievementIds) {
                summary.put(achievementId, "completed");
            }
            return Map.copyOf(summary);
        }
    }
}
