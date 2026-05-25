package com.poro.empire.boss.db;

import com.poro.empire.boss.engine.BossResultSummary;
import com.poro.empire.boss.engine.BossRun;
import com.poro.empire.boss.engine.BossRunRecordHook;
import com.poro.empire.common.logging.DomainLogger;
import com.poro.empire.common.result.Result;

import java.util.Objects;

public final class DbBossRunRecordHook implements BossRunRecordHook {
    private final BossSessionRepository repository;
    private final long seasonStartEpoch;
    private final DomainLogger logger;

    public DbBossRunRecordHook(BossSessionRepository repository, long seasonStartEpoch, DomainLogger logger) {
        this.repository = Objects.requireNonNull(repository, "repository");
        this.seasonStartEpoch = seasonStartEpoch;
        this.logger = Objects.requireNonNull(logger, "logger");
    }

    @Override
    public void onRunStarted(BossRun run) {
        // session is recorded atomically on end; no insert on start
    }

    @Override
    public void onRunEnded(BossResultSummary summary) {
        int week = computeSeasonWeek(summary.startedAt());
        Result<Void> result = repository.record(summary, week);
        if (result.isFailure()) {
            logger.warn("Boss session DB record failed. run_id=" + summary.runId()
                    + ", boss_id=" + summary.bossId() + ": " + result.message());
        }
    }

    private int computeSeasonWeek(long startedAt) {
        if (seasonStartEpoch <= 0 || startedAt < seasonStartEpoch) return 1;
        return (int) ((startedAt - seasonStartEpoch) / (7L * 24 * 3600)) + 1;
    }
}
