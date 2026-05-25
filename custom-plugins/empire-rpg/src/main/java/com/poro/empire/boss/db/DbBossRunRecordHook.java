package com.poro.empire.boss.db;

import com.poro.empire.boss.engine.BossResultSummary;
import com.poro.empire.boss.engine.BossRun;
import com.poro.empire.boss.engine.BossRunRecordHook;
import com.poro.empire.common.logging.DomainLogger;
import com.poro.empire.common.result.Result;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public final class DbBossRunRecordHook implements BossRunRecordHook {
    private final BossSessionRepository repository;
    private final long seasonStartEpoch;
    private final DomainLogger logger;
    private final ConcurrentHashMap<String, Long> runIdToSessionId = new ConcurrentHashMap<>();

    public DbBossRunRecordHook(BossSessionRepository repository, long seasonStartEpoch, DomainLogger logger) {
        this.repository = Objects.requireNonNull(repository, "repository");
        this.seasonStartEpoch = seasonStartEpoch;
        this.logger = Objects.requireNonNull(logger, "logger");
    }

    @Override
    public void onRunStarted(BossRun run) {
        int week = computeSeasonWeek(run.enteredAt().getEpochSecond());
        Result<Long> sessionResult = repository.recordStart(
                run.bossId(), run.partySize(), week, run.enteredAt().getEpochSecond());
        if (sessionResult.isFailure()) {
            logger.warn("Failed to record boss session start. run_id=" + run.runId()
                    + ", boss_id=" + run.bossId() + ": " + sessionResult.message());
            return;
        }
        long sessionId = sessionResult.value();
        runIdToSessionId.put(run.runId(), sessionId);
        for (String uuid : run.participants()) {
            Result<Void> playerResult = repository.recordPlayerEntry(sessionId, uuid);
            if (playerResult.isFailure()) {
                logger.warn("Failed to record player entry. session_id=" + sessionId
                        + ", uuid=" + uuid + ": " + playerResult.message());
            }
        }
    }

    @Override
    public void onRunEnded(BossResultSummary summary) {
        Long sessionId = runIdToSessionId.remove(summary.runId());
        if (sessionId == null) {
            logger.warn("No DB session_id for run_id=" + summary.runId() + " — end record skipped.");
            return;
        }
        Result<Void> result = repository.recordEnd(sessionId, summary);
        if (result.isFailure()) {
            logger.warn("Failed to record boss session end. session_id=" + sessionId
                    + ", run_id=" + summary.runId() + ": " + result.message());
        }
    }

    private int computeSeasonWeek(long startedAt) {
        if (seasonStartEpoch <= 0 || startedAt < seasonStartEpoch) return 1;
        return (int) ((startedAt - seasonStartEpoch) / (7L * 24 * 3600)) + 1;
    }
}
