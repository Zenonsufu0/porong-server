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
    private final BossParticipantSpecResolver specResolver;
    private final com.poro.empire.boss.room.BossDamageTracker damageTracker;
    private final ConcurrentHashMap<String, Long> runIdToSessionId = new ConcurrentHashMap<>();

    public DbBossRunRecordHook(BossSessionRepository repository, long seasonStartEpoch,
                               DomainLogger logger, BossParticipantSpecResolver specResolver,
                               com.poro.empire.boss.room.BossDamageTracker damageTracker) {
        this.repository = Objects.requireNonNull(repository, "repository");
        this.seasonStartEpoch = seasonStartEpoch;
        this.logger = Objects.requireNonNull(logger, "logger");
        this.specResolver = specResolver != null ? specResolver : BossParticipantSpecResolver.ZERO;
        this.damageTracker = damageTracker;
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

        // 참여자 실측 스펙 기록 + 파티 평균 집계 (DL-081)
        double enhanceSum = 0.0;
        double ilSum = 0.0;
        int counted = 0;
        for (String uuid : run.participants()) {
            BossParticipantSpec spec = specResolver.resolve(uuid);
            Result<Void> playerResult = repository.recordPlayerEntry(sessionId, uuid, spec);
            if (playerResult.isFailure()) {
                logger.warn("Failed to record player entry. session_id=" + sessionId
                        + ", uuid=" + uuid + ": " + playerResult.message());
            }
            enhanceSum += spec.avgEnhance();
            ilSum += spec.il();
            counted++;
        }
        if (counted > 0) {
            double partyAvgEnhance = enhanceSum / counted;
            double partyAvgIl = ilSum / counted;
            Result<Void> partyResult = repository.recordPartySpec(sessionId, partyAvgEnhance, partyAvgIl);
            if (partyResult.isFailure()) {
                logger.warn("Failed to record party spec. session_id=" + sessionId
                        + ": " + partyResult.message());
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

        // 참여자 데미지 기여 점유율 기록 (DL-084) — 추적된 보스 mob 데미지 → boss_session_player
        if (damageTracker != null) {
            damageTracker.finalizeShares(summary.runId()).forEach((playerUuid, share) -> {
                Result<Void> dmgResult = repository.recordPlayerDamage(sessionId, playerUuid.toString(), share);
                if (dmgResult.isFailure()) {
                    logger.warn("Failed to record damage share. session_id=" + sessionId
                            + ", uuid=" + playerUuid + ": " + dmgResult.message());
                }
            });
        }
    }

    private int computeSeasonWeek(long startedAt) {
        if (seasonStartEpoch <= 0 || startedAt < seasonStartEpoch) return 1;
        return (int) ((startedAt - seasonStartEpoch) / (7L * 24 * 3600)) + 1;
    }
}
