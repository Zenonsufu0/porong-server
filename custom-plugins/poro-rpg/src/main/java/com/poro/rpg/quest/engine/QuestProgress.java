package com.poro.rpg.quest.engine;

import java.time.Instant;
import java.util.Locale;
import java.util.Objects;

public final class QuestProgress {
    private final String questId;
    private final QuestObjectiveType objectiveType;
    private final String objectiveTargetId;
    private final long requiredAmount;
    private final Instant startedAt;

    private long currentAmount;
    private Instant completedAt;

    public QuestProgress(
            String questId,
            QuestObjectiveType objectiveType,
            String objectiveTargetId,
            long requiredAmount,
            Instant startedAt
    ) {
        this.questId = normalize(questId);
        this.objectiveType = Objects.requireNonNull(objectiveType, "objectiveType");
        this.objectiveTargetId = normalize(objectiveTargetId);
        this.requiredAmount = Math.max(1L, requiredAmount);
        this.startedAt = Objects.requireNonNull(startedAt, "startedAt");
        this.currentAmount = 0L;
    }

    public String questId() {
        return questId;
    }

    public QuestObjectiveType objectiveType() {
        return objectiveType;
    }

    public String objectiveTargetId() {
        return objectiveTargetId;
    }

    public long requiredAmount() {
        return requiredAmount;
    }

    public long currentAmount() {
        return currentAmount;
    }

    public Instant startedAt() {
        return startedAt;
    }

    public Instant completedAt() {
        return completedAt;
    }

    public boolean isCompleted() {
        return completedAt != null || currentAmount >= requiredAmount;
    }

    public long remainingAmount() {
        return Math.max(0L, requiredAmount - currentAmount);
    }

    public void increase(long amount) {
        if (amount <= 0L || isCompleted()) {
            return;
        }
        currentAmount = Math.min(requiredAmount, currentAmount + amount);
    }

    public void markCompleted(Instant completedAt) {
        this.currentAmount = requiredAmount;
        this.completedAt = completedAt;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
