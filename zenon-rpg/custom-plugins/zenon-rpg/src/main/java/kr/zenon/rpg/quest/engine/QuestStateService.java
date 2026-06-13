package kr.zenon.rpg.quest.engine;

import kr.zenon.rpg.common.registry.master.QuestMasterRegistry;
import kr.zenon.rpg.common.registry.master.model.QuestMaster;
import kr.zenon.rpg.common.result.ErrorCode;
import kr.zenon.rpg.common.result.Result;
import kr.zenon.rpg.common.time.TimeProvider;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class QuestStateService {
    private final QuestMasterRegistry questMasterRegistry;
    private final QuestObjectiveEvaluator objectiveEvaluator;
    private final QuestRewardResolver rewardResolver;
    private final QuestChainService chainService;
    private final NextQuestUnlockHook nextQuestUnlockHook;
    private final AchievementTracker achievementTracker;
    private final TimeProvider timeProvider;

    public QuestStateService(
            QuestMasterRegistry questMasterRegistry,
            QuestObjectiveEvaluator objectiveEvaluator,
            QuestRewardResolver rewardResolver,
            QuestChainService chainService,
            NextQuestUnlockHook nextQuestUnlockHook,
            AchievementTracker achievementTracker,
            TimeProvider timeProvider
    ) {
        this.questMasterRegistry = Objects.requireNonNull(questMasterRegistry, "questMasterRegistry");
        this.objectiveEvaluator = Objects.requireNonNull(objectiveEvaluator, "objectiveEvaluator");
        this.rewardResolver = Objects.requireNonNull(rewardResolver, "rewardResolver");
        this.chainService = Objects.requireNonNull(chainService, "chainService");
        this.nextQuestUnlockHook = Objects.requireNonNull(nextQuestUnlockHook, "nextQuestUnlockHook");
        this.achievementTracker = Objects.requireNonNull(achievementTracker, "achievementTracker");
        this.timeProvider = Objects.requireNonNull(timeProvider, "timeProvider");
    }

    public Result<StartResult> startQuest(PlayerQuestHonorState state, String questId) {
        if (state == null) {
            return Result.failure(ErrorCode.INVALID_ARGUMENT, "state is required.");
        }
        QuestMaster quest = questMasterRegistry.find(questId).orElse(null);
        if (quest == null) {
            return Result.failure(ErrorCode.INVALID_ARGUMENT, "Unknown quest_id: " + questId);
        }

        String normalizedQuestId = normalize(quest.questId());
        if (state.activeQuest(normalizedQuestId).isPresent()) {
            return Result.failure(ErrorCode.INVALID_ARGUMENT, "Quest is already active: " + normalizedQuestId);
        }
        if (!isRepeatableQuest(quest) && state.completedQuestCount(normalizedQuestId) > 0) {
            return Result.failure(ErrorCode.INVALID_ARGUMENT, "Quest is already completed: " + normalizedQuestId);
        }
        if (!isUnlockSatisfied(state, quest) && !state.isQuestUnlocked(normalizedQuestId)) {
            return Result.failure(
                    ErrorCode.INVALID_ARGUMENT,
                    "Quest unlock condition is not satisfied. quest_id=" + normalizedQuestId
            );
        }

        QuestObjectiveType objectiveType;
        try {
            objectiveType = QuestObjectiveType.from(quest.objectiveType());
        } catch (Exception exception) {
            return Result.failure(ErrorCode.INVALID_ARGUMENT, "Unknown objective type: " + quest.objectiveType(), exception);
        }

        QuestProgress progress = new QuestProgress(
                normalizedQuestId,
                objectiveType,
                quest.objectiveTargetId(),
                Math.max(1, quest.objectiveAmount()),
                timeProvider.nowInstant()
        );
        state.startQuest(progress);

        return Result.success(new StartResult(
                normalizedQuestId,
                normalize(quest.questType()),
                progress.objectiveType(),
                progress.objectiveTargetId(),
                progress.requiredAmount(),
                progress.startedAt()
        ));
    }

    public Result<ProgressUpdateBatch> updateProgress(PlayerQuestHonorState state, QuestEvent event) {
        if (state == null || event == null) {
            return Result.failure(ErrorCode.INVALID_ARGUMENT, "state and event are required.");
        }

        if (event.eventType() == ProgressEventType.ENTER_REGION || event.eventType() == ProgressEventType.REGION_DISCOVER) {
            state.discoverRegion(event.targetId());
        }

        Map<String, QuestProgressUpdate> progressUpdates = new LinkedHashMap<>();
        List<QuestCompletionResult> completionResults = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        List<String> activeQuestIds = new ArrayList<>(state.activeQuestsSnapshot().keySet());
        for (String questId : activeQuestIds) {
            QuestProgress progress = state.activeQuest(questId).orElse(null);
            if (progress == null) {
                continue;
            }
            QuestMaster quest = questMasterRegistry.find(questId).orElse(null);
            if (quest == null) {
                warnings.add("Active quest not found in master registry: " + questId);
                continue;
            }

            Result<QuestObjectiveEvaluator.EvaluationResult> evaluation = objectiveEvaluator.evaluate(quest, progress, event);
            if (evaluation.isFailure()) {
                warnings.add("Objective evaluation failed for quest_id=" + questId + ": " + evaluation.message());
                continue;
            }
            QuestObjectiveEvaluator.EvaluationResult result = evaluation.value();
            if (!result.matched()) {
                continue;
            }

            progress.increase(result.increment());
            progressUpdates.put(questId, new QuestProgressUpdate(
                    questId,
                    result.increment(),
                    progress.currentAmount(),
                    progress.requiredAmount(),
                    progress.remainingAmount()
            ));

            if (progress.isCompleted()) {
                progress.markCompleted(timeProvider.nowInstant());
                Result<QuestCompletionResult> completion = completeQuest(state, questId);
                if (completion.isFailure()) {
                    warnings.add("Quest completion failed for quest_id=" + questId + ": " + completion.message());
                    continue;
                }
                completionResults.add(completion.value());
            }
        }

        Result<AchievementTracker.TrackingResult> trackingResult = achievementTracker.onEvent(state, event);
        AchievementTracker.TrackingResult achievementUpdate = trackingResult.isOk()
                ? trackingResult.value()
                : new AchievementTracker.TrackingResult(event, List.of(), List.of(), List.of(trackingResult.message()));
        if (trackingResult.isFailure()) {
            warnings.add("Achievement tracking failed: " + trackingResult.message());
        }
        warnings.addAll(achievementUpdate.warnings());

        return Result.success(new ProgressUpdateBatch(
                event,
                Map.copyOf(progressUpdates),
                List.copyOf(completionResults),
                achievementUpdate,
                List.copyOf(warnings)
        ));
    }

    public Result<QuestCompletionResult> completeQuest(PlayerQuestHonorState state, String questId) {
        if (state == null) {
            return Result.failure(ErrorCode.INVALID_ARGUMENT, "state is required.");
        }
        QuestMaster quest = questMasterRegistry.find(questId).orElse(null);
        if (quest == null) {
            return Result.failure(ErrorCode.INVALID_ARGUMENT, "Unknown quest_id: " + questId);
        }
        QuestProgress progress = state.activeQuest(questId).orElse(null);
        if (progress == null) {
            return Result.failure(ErrorCode.INVALID_ARGUMENT, "Quest is not active: " + questId);
        }
        if (!progress.isCompleted()) {
            return Result.failure(ErrorCode.INVALID_ARGUMENT, "Quest objective is not completed yet: " + questId);
        }

        Instant now = timeProvider.nowInstant();
        state.markQuestCompleted(quest.questId(), now);

        Result<QuestRewardResolver.RewardResolution> rewardResolution = rewardResolver.resolve(state, quest);
        if (rewardResolution.isFailure()) {
            return Result.failure(rewardResolution.errorCode(), rewardResolution.message(), rewardResolution.cause());
        }

        List<String> nextQuestIds = chainService.resolveNextQuestIds(quest.questId());
        for (String nextQuestId : nextQuestIds) {
            state.unlockQuest(nextQuestId);
        }
        nextQuestUnlockHook.onNextQuestsUnlocked(state, normalize(quest.questId()), nextQuestIds);

        // feed quest clear event to achievement tracker
        achievementTracker.onEvent(state, QuestEvent.of(ProgressEventType.QUEST_CLEAR, quest.questId(), 1L));

        return Result.success(new QuestCompletionResult(
                normalize(quest.questId()),
                normalize(quest.questType()),
                rewardResolution.value(),
                List.copyOf(nextQuestIds),
                now
        ));
    }

    public Set<String> completedQuestIds(PlayerQuestHonorState state) {
        return state == null ? Set.of() : state.completedQuestIds();
    }

    private boolean isUnlockSatisfied(PlayerQuestHonorState state, QuestMaster quest) {
        String conditionType = normalize(quest.unlockConditionType());
        String conditionValue = normalize(quest.unlockConditionValue());
        if (conditionType.isBlank() || "none".equals(conditionType) || "-".equals(conditionType)) {
            return true;
        }

        return switch (conditionType) {
            case "quest_clear" -> state.completedQuestCount(conditionValue) > 0;
            case "region_discover" -> state.isRegionDiscovered(conditionValue);
            case "achievement_clear" -> state.isAchievementCompleted(conditionValue);
            case "unlock" -> state.hasUnlockFlag(conditionValue);
            default -> false;
        };
    }

    private boolean isRepeatableQuest(QuestMaster quest) {
        return "daily".equalsIgnoreCase(quest.questType());
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    public record StartResult(
            String questId,
            String questType,
            QuestObjectiveType objectiveType,
            String objectiveTargetId,
            long objectiveRequiredAmount,
            Instant startedAt
    ) {
    }

    public record QuestProgressUpdate(
            String questId,
            long increment,
            long currentAmount,
            long requiredAmount,
            long remainingAmount
    ) {
    }

    public record QuestCompletionResult(
            String questId,
            String questType,
            QuestRewardResolver.RewardResolution rewardResolution,
            List<String> nextQuestIds,
            Instant completedAt
    ) {
    }

    public record ProgressUpdateBatch(
            QuestEvent event,
            Map<String, QuestProgressUpdate> progressUpdates,
            List<QuestCompletionResult> completedQuests,
            AchievementTracker.TrackingResult achievementTrackingResult,
            List<String> warnings
    ) {
    }
}
