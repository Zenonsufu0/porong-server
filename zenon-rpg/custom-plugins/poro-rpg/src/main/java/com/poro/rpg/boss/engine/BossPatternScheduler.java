package com.poro.rpg.boss.engine;

import com.poro.rpg.common.result.ErrorCode;
import com.poro.rpg.common.result.Result;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public final class BossPatternScheduler {
    private final BossPatternRegistry patternRegistry;

    public BossPatternScheduler(BossPatternRegistry patternRegistry) {
        this.patternRegistry = Objects.requireNonNull(patternRegistry, "patternRegistry");
    }

    public void enqueueForced(BossRun run, String patternId) {
        if (patternId == null || patternId.isBlank()) {
            return;
        }
        run.enqueueForcedPattern(patternId.trim().toLowerCase(Locale.ROOT));
    }

    public void enqueueForced(BossRun run, List<String> patternIds) {
        for (String patternId : patternIds) {
            enqueueForced(run, patternId);
        }
    }

    public Result<PatternSelection> select(BossRun run, Instant now) {
        String forcedPatternId = run.pollForcedPattern();
        if (forcedPatternId != null) {
            return selectForced(run, now, forcedPatternId);
        }

        List<BossPattern> candidates = patternRegistry.findByBossAndPhase(run.bossId(), run.currentPhase()).stream()
                .filter(pattern -> !shouldSkipByConditionType(pattern))
                .filter(pattern -> isConditionSatisfied(pattern, run, now))
                .filter(pattern -> isCooldownReady(pattern, run, now))
                .filter(pattern -> isConsecutiveAllowed(pattern, run))
                .sorted(Comparator.comparingInt(BossPattern::priorityScore).reversed().thenComparing(BossPattern::patternId))
                .toList();

        if (candidates.isEmpty()) {
            return Result.failure(
                    ErrorCode.INVALID_ARGUMENT,
                    "No available pattern for boss_id=" + run.bossId() + ", phase=" + run.currentPhase()
            );
        }

        BossPattern selected = candidates.get(0);
        run.markPatternUsed(selected.patternId(), now);
        return Result.success(new PatternSelection(selected.patternId(), false, "priority_select"));
    }

    private Result<PatternSelection> selectForced(BossRun run, Instant now, String patternId) {
        if ("boss_clear".equals(patternId) || "battle_fail".equals(patternId)) {
            return Result.success(new PatternSelection(patternId, true, "forced_terminal"));
        }

        if (patternRegistry.find(run.bossId(), patternId).isEmpty()) {
            return Result.failure(
                    ErrorCode.INVALID_ARGUMENT,
                    "Forced pattern not found: boss_id=" + run.bossId() + ", pattern_id=" + patternId
            );
        }

        run.markPatternUsed(patternId, now);
        return Result.success(new PatternSelection(patternId, true, "forced_queue"));
    }

    private boolean shouldSkipByConditionType(BossPattern pattern) {
        String conditionType = pattern.conditionType();
        return "PHASE_ENTER".equals(conditionType) || "HP_ZERO".equals(conditionType);
    }

    private boolean isConditionSatisfied(BossPattern pattern, BossRun run, Instant now) {
        String conditionType = pattern.conditionType();
        String conditionValue = pattern.conditionValue();
        return switch (conditionType) {
            case "", "NONE" -> true;
            case "TIME_ELAPSED" -> elapsedSeconds(run, now) >= parseDouble(conditionValue, 0.0d);
            case "HP_BELOW" -> run.bossHpPercent() <= parseDouble(conditionValue, 100.0d);
            case "PHASE_ENTER", "HP_ZERO" -> false;
            default -> true;
        };
    }

    private boolean isCooldownReady(BossPattern pattern, BossRun run, Instant now) {
        if (pattern.cooldownSeconds() <= 0) {
            return true;
        }
        Instant lastUsedAt = run.lastUsedAt(pattern.patternId());
        if (lastUsedAt == null) {
            return true;
        }
        long elapsed = Duration.between(lastUsedAt, now).toSeconds();
        return elapsed >= pattern.cooldownSeconds();
    }

    private boolean isConsecutiveAllowed(BossPattern pattern, BossRun run) {
        if (!pattern.patternId().equals(run.lastPatternId())) {
            return true;
        }
        int maxConsecutive = Math.max(1, pattern.maxConsecutiveUse());
        return run.consecutivePatternUse() < maxConsecutive;
    }

    private long elapsedSeconds(BossRun run, Instant now) {
        return Math.max(0L, Duration.between(run.enteredAt(), now).toSeconds());
    }

    private double parseDouble(String raw, double fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        try {
            return Double.parseDouble(raw);
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    public record PatternSelection(
            String patternId,
            boolean forced,
            String reason
    ) {
    }
}
