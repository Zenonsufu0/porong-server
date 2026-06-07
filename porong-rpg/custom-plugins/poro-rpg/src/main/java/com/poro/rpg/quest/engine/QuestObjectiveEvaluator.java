package com.poro.rpg.quest.engine;

import com.poro.rpg.common.registry.master.model.QuestMaster;
import com.poro.rpg.common.result.ErrorCode;
import com.poro.rpg.common.result.Result;

import java.util.Locale;

public final class QuestObjectiveEvaluator {
    public Result<EvaluationResult> evaluate(QuestMaster quest, QuestProgress progress, QuestEvent event) {
        if (quest == null || progress == null || event == null) {
            return Result.failure(ErrorCode.INVALID_ARGUMENT, "quest, progress, event are required.");
        }
        if (progress.isCompleted()) {
            return Result.success(new EvaluationResult(false, 0L, progress.currentAmount(), progress.requiredAmount()));
        }

        QuestObjectiveType objectiveType;
        try {
            objectiveType = QuestObjectiveType.from(quest.objectiveType());
        } catch (Exception exception) {
            return Result.failure(ErrorCode.INVALID_ARGUMENT, "Unknown objective_type: " + quest.objectiveType(), exception);
        }

        boolean matched = matches(objectiveType, progress.objectiveTargetId(), event);
        if (!matched) {
            return Result.success(new EvaluationResult(false, 0L, progress.currentAmount(), progress.requiredAmount()));
        }

        long increment = incrementAmount(objectiveType, event.amount());
        return Result.success(new EvaluationResult(true, increment, progress.currentAmount(), progress.requiredAmount()));
    }

    private boolean matches(QuestObjectiveType objectiveType, String objectiveTargetId, QuestEvent event) {
        return switch (objectiveType) {
            case TALK -> event.eventType() == ProgressEventType.TALK && targetMatches(objectiveTargetId, event.targetId());
            case KILL -> event.eventType() == ProgressEventType.KILL && targetMatches(objectiveTargetId, event.targetId());
            case COLLECT -> event.eventType() == ProgressEventType.COLLECT && targetMatches(objectiveTargetId, event.targetId());
            case GATHER -> event.eventType() == ProgressEventType.GATHER && targetMatches(objectiveTargetId, event.targetId());
            case CRAFT -> event.eventType() == ProgressEventType.CRAFT && targetMatches(objectiveTargetId, event.targetId());
            case DELIVER -> event.eventType() == ProgressEventType.DELIVER && targetMatches(objectiveTargetId, event.targetId());
            case INTERACT -> event.eventType() == ProgressEventType.INTERACT && targetMatches(objectiveTargetId, event.targetId());
            case ENTER_REGION -> (event.eventType() == ProgressEventType.ENTER_REGION || event.eventType() == ProgressEventType.REGION_DISCOVER)
                    && targetMatches(objectiveTargetId, event.targetId());
            case ENTER_DUNGEON -> event.eventType() == ProgressEventType.ENTER_DUNGEON && targetMatches(objectiveTargetId, event.targetId());
            case CLEAR_BOSS -> (event.eventType() == ProgressEventType.CLEAR_BOSS
                    || event.eventType() == ProgressEventType.BOSS_CLEAR
                    || event.eventType() == ProgressEventType.EXTREME_CLEAR)
                    && targetMatches(objectiveTargetId, event.targetId());
            case VIEW_UI -> event.eventType() == ProgressEventType.VIEW_UI && targetMatches(objectiveTargetId, event.targetId());
            case INSTALL_FACILITY -> event.eventType() == ProgressEventType.INSTALL_FACILITY && targetMatches(objectiveTargetId, event.targetId());
            case HARVEST -> event.eventType() == ProgressEventType.HARVEST && targetMatches(objectiveTargetId, event.targetId());
            case EQUIP_ITEM -> event.eventType() == ProgressEventType.EQUIP_ITEM && targetMatches(objectiveTargetId, event.targetId());
            case ENHANCE_ITEM -> event.eventType() == ProgressEventType.ENHANCE_ITEM && targetMatches(objectiveTargetId, event.targetId());
        };
    }

    private long incrementAmount(QuestObjectiveType objectiveType, long eventAmount) {
        long safeAmount = Math.max(1L, eventAmount);
        return switch (objectiveType) {
            case TALK, INTERACT, ENTER_REGION, ENTER_DUNGEON, VIEW_UI, INSTALL_FACILITY, EQUIP_ITEM -> 1L;
            case KILL, COLLECT, GATHER, CRAFT, DELIVER, CLEAR_BOSS, HARVEST, ENHANCE_ITEM -> safeAmount;
        };
    }

    private boolean targetMatches(String objectiveTargetId, String eventTargetId) {
        String expected = normalize(objectiveTargetId);
        if (expected.isBlank() || "any".equals(expected) || "none".equals(expected) || "-".equals(expected)) {
            return true;
        }
        return expected.equals(normalize(eventTargetId));
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    public record EvaluationResult(
            boolean matched,
            long increment,
            long currentAmount,
            long requiredAmount
    ) {
    }
}
