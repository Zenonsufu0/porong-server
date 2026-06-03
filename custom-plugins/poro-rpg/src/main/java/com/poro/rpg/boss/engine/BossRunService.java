package com.poro.rpg.boss.engine;

import com.poro.rpg.common.ids.CommonIds;
import com.poro.rpg.common.logging.DomainLogger;
import com.poro.rpg.common.result.ErrorCode;
import com.poro.rpg.common.result.Result;
import com.poro.rpg.common.time.TimeProvider;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class BossRunService {
    private final BossEntryValidator entryValidator;
    private final BossPhaseController phaseController;
    private final BossPatternScheduler patternScheduler;
    private final BossStaggerResolver staggerResolver;
    private final BossBerserkService berserkService;
    private final BossDeathCountService deathCountService;
    private final BossResultSummaryBuilder resultSummaryBuilder;
    private final BossRunRecordHook runRecordHook;
    private final BossRewardResolverHook rewardResolverHook;
    private final TimeProvider timeProvider;
    private final DomainLogger logger;
    private final Map<String, BossRun> activeRuns = new ConcurrentHashMap<>();
    private final Map<String, String> activeRunByParticipant = new ConcurrentHashMap<>();

    public BossRunService(
            BossEntryValidator entryValidator,
            BossPhaseController phaseController,
            BossPatternScheduler patternScheduler,
            BossStaggerResolver staggerResolver,
            BossBerserkService berserkService,
            BossDeathCountService deathCountService,
            BossResultSummaryBuilder resultSummaryBuilder,
            BossRunRecordHook runRecordHook,
            BossRewardResolverHook rewardResolverHook,
            TimeProvider timeProvider,
            DomainLogger logger
    ) {
        this.entryValidator = Objects.requireNonNull(entryValidator, "entryValidator");
        this.phaseController = Objects.requireNonNull(phaseController, "phaseController");
        this.patternScheduler = Objects.requireNonNull(patternScheduler, "patternScheduler");
        this.staggerResolver = Objects.requireNonNull(staggerResolver, "staggerResolver");
        this.berserkService = Objects.requireNonNull(berserkService, "berserkService");
        this.deathCountService = Objects.requireNonNull(deathCountService, "deathCountService");
        this.resultSummaryBuilder = Objects.requireNonNull(resultSummaryBuilder, "resultSummaryBuilder");
        this.runRecordHook = Objects.requireNonNull(runRecordHook, "runRecordHook");
        this.rewardResolverHook = Objects.requireNonNull(rewardResolverHook, "rewardResolverHook");
        this.timeProvider = Objects.requireNonNull(timeProvider, "timeProvider");
        this.logger = Objects.requireNonNull(logger, "logger");
    }

    public Result<BossRun> startRun(BossEntryRequest request) {
        Result<BossEntryRule> validation = entryValidator.validate(request, this::isUserInActiveRun);
        if (validation.isFailure()) {
            return Result.failure(validation.errorCode(), validation.message(), validation.cause());
        }

        String runId = CommonIds.newRunId();
        int totalDeathCount = deathCountService.initialDeathCount(request.partySize());
        BossRun run = new BossRun(
                runId,
                request.bossId(),
                request.leaderUserId(),
                request.partyMemberIds(),
                timeProvider.nowInstant(),
                totalDeathCount
        );

        BossPhaseController.PhaseUpdate phaseUpdate = phaseController.initialize(run);
        patternScheduler.enqueueForced(run, phaseUpdate.forcedPatternIds());

        activeRuns.put(runId, run);
        for (String participantId : request.partyMemberIds()) {
            activeRunByParticipant.put(participantId, runId);
        }

        runRecordHook.onRunStarted(run);
        logger.info("Boss run started. run_id=" + run.runId() + ", boss_id=" + run.bossId() + ", party_size=" + run.partySize());
        return Result.success(run);
    }

    public Result<BossPhaseController.PhaseUpdate> updateBossHp(String runId, double bossHpPercent) {
        Result<BossRun> runResult = findActiveRun(runId);
        if (runResult.isFailure()) {
            return Result.failure(runResult.errorCode(), runResult.message(), runResult.cause());
        }

        BossRun run = runResult.value();
        BossPhaseController.PhaseUpdate phaseUpdate = phaseController.evaluate(run, bossHpPercent);
        patternScheduler.enqueueForced(run, phaseUpdate.forcedPatternIds());

        BossBerserkService.BerserkTransition berserkTransition = berserkService.enterIfNeeded(run);
        if (berserkTransition.entered()) {
            patternScheduler.enqueueForced(run, berserkTransition.forcedPatternIds());
        }

        return Result.success(phaseUpdate);
    }

    public Result<BossPatternScheduler.PatternSelection> selectNextPattern(String runId) {
        Result<BossRun> runResult = findActiveRun(runId);
        if (runResult.isFailure()) {
            return Result.failure(runResult.errorCode(), runResult.message(), runResult.cause());
        }
        return patternScheduler.select(runResult.value(), timeProvider.nowInstant());
    }

    public Result<BossStaggerResolver.StaggerResolution> resolveStagger(String runId, String triggerPatternId, boolean success) {
        Result<BossRun> runResult = findActiveRun(runId);
        if (runResult.isFailure()) {
            return Result.failure(runResult.errorCode(), runResult.message(), runResult.cause());
        }
        BossRun run = runResult.value();

        Result<BossStaggerResolver.StaggerResolution> resolutionResult = staggerResolver.resolve(run, triggerPatternId, success);
        if (resolutionResult.isFailure()) {
            return resolutionResult;
        }

        BossStaggerResolver.StaggerResolution resolution = resolutionResult.value();
        if (resolution.branchPatternId() != null && !resolution.branchPatternId().isBlank()) {
            patternScheduler.enqueueForced(run, resolution.branchPatternId());
        }

        if (resolution.terminal()) {
            if ("boss_clear".equals(resolution.terminalResultCode())) {
                endRun(run.runId(), true, "");
            } else if ("battle_fail".equals(resolution.terminalResultCode())) {
                endRun(run.runId(), false, "fail_stagger_fail");
            }
        }

        return Result.success(resolution);
    }

    public Result<BossBerserkService.BerserkOutcome> resolveBerserk(String runId, boolean success) {
        Result<BossRun> runResult = findActiveRun(runId);
        if (runResult.isFailure()) {
            return Result.failure(runResult.errorCode(), runResult.message(), runResult.cause());
        }

        BossRun run = runResult.value();
        BossBerserkService.BerserkOutcome outcome = berserkService.resolveOutcome(run, success);
        endRun(run.runId(), outcome.clearSuccess(), outcome.failureReasonCode());
        return Result.success(outcome);
    }

    public Result<BossDeathCountService.DeathCountUpdate> registerDeath(String runId, String userId, boolean partyWipe) {
        Result<BossRun> runResult = findActiveRun(runId);
        if (runResult.isFailure()) {
            return Result.failure(runResult.errorCode(), runResult.message(), runResult.cause());
        }

        BossRun run = runResult.value();
        Result<BossDeathCountService.DeathCountUpdate> update = deathCountService.registerDeath(run, userId, partyWipe);
        if (update.isFailure()) {
            return update;
        }

        BossDeathCountService.DeathCountUpdate deathUpdate = update.value();
        if (deathUpdate.battleFailed()) {
            endRun(runId, false, deathUpdate.failureReasonCode());
        }
        return Result.success(deathUpdate);
    }

    public Result<BossResultSummary> endRun(String runId, boolean clearSuccess, String failureReasonCode) {
        Result<BossRun> runResult = findActiveRun(runId);
        if (runResult.isFailure()) {
            return Result.failure(runResult.errorCode(), runResult.message(), runResult.cause());
        }

        BossRun run = runResult.value();
        run.end(timeProvider.nowInstant(), clearSuccess, failureReasonCode);

        BossResultSummary summary = resultSummaryBuilder.fromRun(run);
        runRecordHook.onRunEnded(summary);
        rewardResolverHook.onRunEnded(summary);

        activeRuns.remove(runId);
        for (String participant : run.participants()) {
            activeRunByParticipant.remove(participant);
        }

        logger.info("Boss run ended. run_id=" + summary.runId()
                + ", clear_success=" + summary.clearSuccess()
                + ", phase_reached=" + summary.phaseReached()
                + ", failure_reason_code=" + summary.failureReasonCode());
        return Result.success(summary);
    }

    /** 시즌 최종보스 3종 — 타임아웃 10분(그 외 15분, 07_boss_pattern_modules §타임아웃 / DL-093). */
    private static final java.util.Set<String> FINAL_BOSSES =
            java.util.Set.of("rift_king", "corrupted_dyad", "spirit_watcher");

    /** 현재 활성 런 스냅샷 (타임아웃 스케줄러용). */
    public java.util.List<BossRun> activeRunsSnapshot() {
        return new java.util.ArrayList<>(activeRuns.values());
    }

    /** 보스 분류별 전투 타임아웃(초): 최종 600 / 그 외 900. */
    public long timeoutSecondsFor(String bossId) {
        return FINAL_BOSSES.contains(bossId) ? 600L : 900L;
    }

    /** enteredAt 기준 타임아웃 경과 여부. */
    public boolean isTimedOut(BossRun run) {
        return java.time.Duration.between(run.enteredAt(), timeProvider.nowInstant()).getSeconds()
                >= timeoutSecondsFor(run.bossId());
    }

    /** runId의 남은 전투 시간(초). 활성 런 없으면 -1, 음수면 0. (스코어보드 타이머 표시용) */
    public long remainingSeconds(String runId) {
        if (runId == null) return -1L;
        BossRun run = activeRuns.get(runId);
        if (run == null) return -1L;
        long elapsed = java.time.Duration.between(run.enteredAt(), timeProvider.nowInstant()).getSeconds();
        return Math.max(0L, timeoutSecondsFor(run.bossId()) - elapsed);
    }

    public boolean isUserInActiveRun(String userId) {
        if (userId == null || userId.isBlank()) {
            return false;
        }
        return activeRunByParticipant.containsKey(userId);
    }

    public Optional<BossRun> findRun(String runId) {
        return Optional.ofNullable(activeRuns.get(runId));
    }

    public Map<String, BossRun> activeRuns() {
        return Map.copyOf(new LinkedHashMap<>(activeRuns));
    }

    public Set<String> activeParticipants() {
        return Set.copyOf(activeRunByParticipant.keySet());
    }

    private Result<BossRun> findActiveRun(String runId) {
        BossRun run = activeRuns.get(runId);
        if (run == null) {
            return Result.failure(ErrorCode.INVALID_ARGUMENT, "Active run not found: " + runId);
        }
        return Result.success(run);
    }
}
