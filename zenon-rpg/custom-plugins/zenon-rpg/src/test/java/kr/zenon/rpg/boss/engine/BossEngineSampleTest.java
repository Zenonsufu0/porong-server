package kr.zenon.rpg.boss.engine;

import kr.zenon.rpg.common.logging.CommonPluginLogger;
import kr.zenon.rpg.common.logging.DomainLogger;
import kr.zenon.rpg.common.registry.master.BossMasterRegistry;
import kr.zenon.rpg.common.registry.master.model.BossMaster;
import kr.zenon.rpg.common.result.Result;
import kr.zenon.rpg.common.seed.CsvSeedLoader;
import kr.zenon.rpg.common.time.TimeProvider;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BossEngineSampleTest {
    @Test
    void shouldRunSampleFlows() {
        FixedTimeProvider timeProvider = new FixedTimeProvider(Instant.parse("2026-01-01T00:00:00Z"));
        InMemoryBossRunRecordHook recordHook = new InMemoryBossRunRecordHook();
        BossRunService runService = createService(timeProvider, recordHook);

        Result<BossRun> ragnesStart = runService.startRun(new BossEntryRequest(
                "fallen_knight",
                "leader_a",
                List.of("leader_a", "member_b", "member_c")
        ));
        assertTrue(ragnesStart.isOk());
        BossRun ragnesRun = ragnesStart.value();

        String ragnesPhase1 = runService.selectNextPattern(ragnesRun.runId()).orElseThrow().patternId();
        timeProvider.advanceSeconds(12);
        runService.updateBossHp(ragnesRun.runId(), 74.0d).orElseThrow();
        String ragnesForcedPhase2 = runService.selectNextPattern(ragnesRun.runId()).orElseThrow().patternId();

        timeProvider.advanceSeconds(10);
        runService.updateBossHp(ragnesRun.runId(), 49.0d).orElseThrow();
        String ragnesStaggerPattern = runService.selectNextPattern(ragnesRun.runId()).orElseThrow().patternId();
        runService.resolveStagger(ragnesRun.runId(), ragnesStaggerPattern, true).orElseThrow();
        String ragnesSuccessBranch = runService.selectNextPattern(ragnesRun.runId()).orElseThrow().patternId();

        timeProvider.advanceSeconds(20);
        runService.updateBossHp(ragnesRun.runId(), 0.0d).orElseThrow();
        String ragnesForcedPhase4 = runService.selectNextPattern(ragnesRun.runId()).orElseThrow().patternId();
        String ragnesBerserkPattern = runService.selectNextPattern(ragnesRun.runId()).orElseThrow().patternId();
        runService.resolveBerserk(ragnesRun.runId(), true).orElseThrow();

        BossResultSummary ragnesSummary = recordHook.summaries().get(0);
        assertTrue(ragnesSummary.clearSuccess());

        Result<BossRun> carmenStart = runService.startRun(new BossEntryRequest(
                "corrupted_lord",
                "leader_x",
                List.of("leader_x", "member_y")
        ));
        assertTrue(carmenStart.isOk());
        BossRun carmenRun = carmenStart.value();

        runService.updateBossHp(carmenRun.runId(), 61.0d).orElseThrow();
        String carmenForcedPhase2 = runService.selectNextPattern(carmenRun.runId()).orElseThrow().patternId();
        String carmenStaggerPattern = runService.selectNextPattern(carmenRun.runId()).orElseThrow().patternId();
        runService.resolveStagger(carmenRun.runId(), carmenStaggerPattern, false).orElseThrow();
        String carmenFailureBranch = runService.selectNextPattern(carmenRun.runId()).orElseThrow().patternId();

        timeProvider.advanceSeconds(18);
        runService.updateBossHp(carmenRun.runId(), 0.0d).orElseThrow();
        String carmenBerserkPattern = runService.selectNextPattern(carmenRun.runId()).orElseThrow().patternId();
        runService.resolveBerserk(carmenRun.runId(), false).orElseThrow();

        BossResultSummary carmenSummary = recordHook.summaries().get(1);
        assertTrue(!carmenSummary.clearSuccess());

        System.out.println("flow1_ragnes="
                + "phase1:" + ragnesPhase1
                + " -> forced_p2:" + ragnesForcedPhase2
                + " -> stagger:" + ragnesStaggerPattern
                + " -> branch:" + ragnesSuccessBranch
                + " -> forced_p4:" + ragnesForcedPhase4
                + " -> berserk:" + ragnesBerserkPattern
                + " -> clear=" + ragnesSummary.clearSuccess()
                + ", phase_reached=" + ragnesSummary.phaseReached()
                + ", clear_time_seconds=" + ragnesSummary.clearTimeSeconds()
                + ", remaining_death_count=" + ragnesSummary.remainingDeathCount());

        System.out.println("flow2_carmen="
                + "forced_p2:" + carmenForcedPhase2
                + " -> stagger:" + carmenStaggerPattern
                + " -> fail_branch:" + carmenFailureBranch
                + " -> berserk:" + carmenBerserkPattern
                + " -> clear=" + carmenSummary.clearSuccess()
                + ", phase_reached=" + carmenSummary.phaseReached()
                + ", failure_reason_code=" + carmenSummary.failureReasonCode());
    }

    @Test
    void shouldApplyDeathCountRules() {
        BossDeathCountService service = new BossDeathCountService();
        assertEquals(3, service.initialDeathCount(1));
        assertEquals(4, service.initialDeathCount(2));
        assertEquals(5, service.initialDeathCount(3));
        System.out.println("deathcount_rule_baseline=party1:3,party2:4,party3:5");

        FixedTimeProvider timeProvider = new FixedTimeProvider(Instant.parse("2026-01-01T01:00:00Z"));
        InMemoryBossRunRecordHook recordHook = new InMemoryBossRunRecordHook();
        BossRunService runService = createService(timeProvider, recordHook);

        BossRun run = runService.startRun(new BossEntryRequest(
                "fallen_knight",
                "solo_user",
                List.of("solo_user")
        )).orElseThrow();

        runService.registerDeath(run.runId(), "solo_user", false).orElseThrow();
        runService.registerDeath(run.runId(), "solo_user", false).orElseThrow();
        BossDeathCountService.DeathCountUpdate update = runService.registerDeath(run.runId(), "solo_user", false).orElseThrow();

        System.out.println("deathcount_rule=party1_total3_after3deaths_remaining=" + update.remainingDeathCount()
                + ", revive_possible=" + update.revivePossible()
                + ", battle_failed=" + update.battleFailed()
                + ", failure_reason=" + update.failureReasonCode());
    }

    private BossRunService createService(FixedTimeProvider timeProvider, InMemoryBossRunRecordHook recordHook) {
        CommonPluginLogger commonLogger = new CommonPluginLogger(Logger.getLogger("BossEngineSampleTest"), "zenon-rpg-test");
        DomainLogger logger = commonLogger.domain("boss-engine-test");

        BossMasterRegistry bossMasterRegistry = new BossMasterRegistry();
        bossMasterRegistry.register(new BossMaster("fallen_knight", "타락 기사장", "season", "field1", true, false));
        bossMasterRegistry.register(new BossMaster("corrupted_lord", "오염된 군주", "season", "field2", true, true));

        BossEntryRuleRegistry entryRuleRegistry = new BossEntryRuleRegistry();
        loadEntryRules().forEach(entryRuleRegistry::register);
        BossPatternRegistry patternRegistry = new BossPatternRegistry();
        loadPatterns().forEach(patternRegistry::register);

        BossEntryValidator entryValidator = new BossEntryValidator(
                bossMasterRegistry,
                entryRuleRegistry,
                new AllowAllUnlockQuestChecker(),
                logger
        );

        return new BossRunService(
                entryValidator,
                new BossPhaseController(patternRegistry),
                new BossPatternScheduler(patternRegistry),
                new BossStaggerResolver(patternRegistry),
                new BossBerserkService(patternRegistry),
                new BossDeathCountService(),
                new BossResultSummaryBuilder(timeProvider),
                recordHook,
                new NoopBossRewardResolverHook(),
                timeProvider,
                logger
        );
    }

    private List<BossEntryRule> loadEntryRules() {
        Path path = Path.of("src/main/resources/seeds/boss_entry_rule.csv");
        CsvSeedLoader<BossEntryRule> loader = new CsvSeedLoader<>("boss_entry_rule", path, BossSeedMappers::entryRule);
        return loader.load().orElseThrow();
    }

    private List<BossPattern> loadPatterns() {
        Path path = Path.of("src/main/resources/seeds/boss_pattern_seed.csv");
        CsvSeedLoader<BossPattern> loader = new CsvSeedLoader<>("boss_pattern_seed", path, BossSeedMappers::pattern);
        return loader.load().orElseThrow();
    }

    private static final class FixedTimeProvider implements TimeProvider {
        private Instant cursor;

        private FixedTimeProvider(Instant cursor) {
            this.cursor = cursor;
        }

        @Override
        public Instant nowInstant() {
            return cursor;
        }

        private void advanceSeconds(long seconds) {
            cursor = cursor.plusSeconds(seconds);
        }
    }
}
