package com.poro.rpg.quest.engine;

import com.poro.rpg.common.registry.master.AchievementMasterRegistry;
import com.poro.rpg.common.registry.master.QuestMasterRegistry;
import com.poro.rpg.common.registry.master.model.AchievementMaster;
import com.poro.rpg.common.registry.master.model.QuestMaster;
import com.poro.rpg.common.time.TimeProvider;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public final class PlayerQuestAndHonorSnapshotBuilder {
    private final QuestMasterRegistry questMasterRegistry;
    private final AchievementMasterRegistry achievementMasterRegistry;
    private final TimeProvider timeProvider;

    public PlayerQuestAndHonorSnapshotBuilder(
            QuestMasterRegistry questMasterRegistry,
            AchievementMasterRegistry achievementMasterRegistry,
            TimeProvider timeProvider
    ) {
        this.questMasterRegistry = Objects.requireNonNull(questMasterRegistry, "questMasterRegistry");
        this.achievementMasterRegistry = Objects.requireNonNull(achievementMasterRegistry, "achievementMasterRegistry");
        this.timeProvider = Objects.requireNonNull(timeProvider, "timeProvider");
    }

    public Snapshot build(PlayerQuestHonorState state) {
        Instant now = timeProvider.nowInstant();
        if (state == null) {
            return new Snapshot("", now, List.of(), 0, List.of(), "", "");
        }

        List<ActiveQuestSummary> activeQuestSummaries = state.activeQuestsSnapshot().values().stream()
                .sorted(Comparator.comparing(QuestProgress::startedAt))
                .map(progress -> {
                    Optional<QuestMaster> quest = questMasterRegistry.find(progress.questId());
                    return new ActiveQuestSummary(
                            progress.questId(),
                            quest.map(QuestMaster::questType).orElse(""),
                            quest.map(QuestMaster::questName).orElse(""),
                            progress.objectiveType().name(),
                            progress.objectiveTargetId(),
                            progress.currentAmount(),
                            progress.requiredAmount(),
                            progress.remainingAmount()
                    );
                })
                .collect(Collectors.toUnmodifiableList());

        List<String> sourceFeatured = state.featuredAchievementIds().isEmpty()
                ? state.recentAchievementIds().stream().limit(3).toList()
                : state.featuredAchievementIds();

        List<FeaturedAchievementSummary> featured = new ArrayList<>();
        for (String achievementId : sourceFeatured) {
            AchievementMaster master = achievementMasterRegistry.find(achievementId).orElse(null);
            if (master == null) {
                continue;
            }
            featured.add(new FeaturedAchievementSummary(
                    master.achievementId(),
                    master.achievementName(),
                    master.category(),
                    master.conditionType(),
                    state.isAchievementCompleted(master.achievementId())
            ));
        }

        return new Snapshot(
                state.userId(),
                now,
                List.copyOf(activeQuestSummaries),
                state.totalCompletedQuestCount(),
                List.copyOf(featured),
                state.equippedTitleId(),
                state.equippedBadgeId()
        );
    }

    public record ActiveQuestSummary(
            String questId,
            String questType,
            String questName,
            String objectiveType,
            String objectiveTargetId,
            long currentAmount,
            long requiredAmount,
            long remainingAmount
    ) {
    }

    public record FeaturedAchievementSummary(
            String achievementId,
            String achievementName,
            String category,
            String conditionType,
            boolean completed
    ) {
    }

    public record Snapshot(
            String userId,
            Instant generatedAt,
            List<ActiveQuestSummary> activeQuestSummaries,
            int completedQuestCount,
            List<FeaturedAchievementSummary> featuredAchievements,
            String equippedTitleId,
            String equippedBadgeId
    ) {
        public Map<String, Object> asWebDto() {
            return Map.of(
                    "user_id", userId,
                    "generated_at", generatedAt.toString(),
                    "active_quests", activeQuestSummaries,
                    "completed_quest_count", completedQuestCount,
                    "featured_achievements", featuredAchievements,
                    "equipped_title_id", equippedTitleId,
                    "equipped_badge_id", equippedBadgeId
            );
        }
    }
}
