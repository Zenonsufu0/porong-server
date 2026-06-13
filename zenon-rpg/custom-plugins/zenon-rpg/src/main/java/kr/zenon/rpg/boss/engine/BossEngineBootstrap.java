package kr.zenon.rpg.boss.engine;

import kr.zenon.rpg.boss.db.BossSessionRepository;
import kr.zenon.rpg.boss.db.DbBossRunRecordHook;
import kr.zenon.rpg.common.config.FoundationContext;
import kr.zenon.rpg.common.logging.DomainLogger;
import kr.zenon.rpg.common.registry.RegistryBootstrapper;
import kr.zenon.rpg.common.registry.master.MasterRegistryContext;
import kr.zenon.rpg.common.result.ErrorCode;
import kr.zenon.rpg.common.result.Result;
import kr.zenon.rpg.common.seed.CsvSeedLoader;
import org.bukkit.plugin.java.JavaPlugin;

import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

public final class BossEngineBootstrap {
    private BossEngineBootstrap() {
    }

    public static Result<BossEngineRuntime> bootstrap(
            JavaPlugin plugin,
            FoundationContext foundationContext,
            MasterRegistryContext masterRegistryContext,
            BossRewardResolverHook rewardResolverHook,
            kr.zenon.rpg.boss.db.BossParticipantSpecResolver participantSpecResolver,
            kr.zenon.rpg.boss.room.BossDamageTracker bossDamageTracker,
            kr.zenon.rpg.boss.room.BossRoomManager bossRoomManager
    ) {
        DomainLogger logger = foundationContext.logger().domain("boss-engine");

        Result<Void> installResult = DefaultBossSeedInstaller.install(plugin, logger);
        if (installResult.isFailure()) {
            return Result.failure(installResult.errorCode(), installResult.message(), installResult.cause());
        }

        BossEntryRuleRegistry entryRuleRegistry = new BossEntryRuleRegistry();
        BossPatternRegistry patternRegistry = new BossPatternRegistry();

        Path seedPath = foundationContext.config().seedPath();
        Result<Void> loadResult = foundationContext.registryBootstrapper().bootstrap(List.of(
                RegistryBootstrapper.task(
                        "boss_entry_rule",
                        new CsvSeedLoader<>("boss_entry_rule", seedPath.resolve("boss_entry_rule.csv"), BossSeedMappers::entryRule),
                        list -> list.forEach(entryRuleRegistry::register)
                ),
                RegistryBootstrapper.task(
                        "boss_pattern_seed",
                        new CsvSeedLoader<>("boss_pattern_seed", seedPath.resolve("boss_pattern_seed.csv"), BossSeedMappers::pattern),
                        list -> list.forEach(patternRegistry::register)
                )
        ));
        if (loadResult.isFailure()) {
            logger.error("Failed to load boss engine seed registries: " + loadResult.message(), loadResult.cause());
            return Result.failure(loadResult.errorCode(), loadResult.message(), loadResult.cause());
        }

        Result<Void> validationResult = validate(masterRegistryContext, entryRuleRegistry, patternRegistry, logger);
        if (validationResult.isFailure()) {
            logger.error("Boss engine seed validation failed: " + validationResult.message(), validationResult.cause());
            return Result.failure(validationResult.errorCode(), validationResult.message(), validationResult.cause());
        }

        BossEntryValidator entryValidator = new BossEntryValidator(
                masterRegistryContext.bossMasters(),
                entryRuleRegistry,
                new BossClearUnlockQuestChecker(bossRoomManager),
                logger
        );
        BossPhaseController phaseController = new BossPhaseController(patternRegistry);
        BossPatternScheduler patternScheduler = new BossPatternScheduler(patternRegistry);
        BossStaggerResolver staggerResolver = new BossStaggerResolver(patternRegistry);
        BossBerserkService berserkService = new BossBerserkService(patternRegistry);
        BossDeathCountService deathCountService = new BossDeathCountService();
        BossSessionRepository bossSessionRepository = new BossSessionRepository(
                foundationContext.connectionProvider(),
                foundationContext.logger().domain("boss-session-repo")
        );
        BossResultSummaryBuilder summaryBuilder = new BossResultSummaryBuilder(foundationContext.timeProvider());
        InMemoryBossRunRecordHook runRecordHook = new InMemoryBossRunRecordHook();
        DbBossRunRecordHook dbRunRecordHook = new DbBossRunRecordHook(
                bossSessionRepository,
                foundationContext.config().seasonStartEpoch(),
                foundationContext.logger().domain("boss-session-db"),
                participantSpecResolver,
                bossDamageTracker
        );
        CompositeBossRunRecordHook compositeRunRecordHook = new CompositeBossRunRecordHook(
                List.of(runRecordHook, dbRunRecordHook)
        );

        BossRunService runService = new BossRunService(
                entryValidator,
                phaseController,
                patternScheduler,
                staggerResolver,
                berserkService,
                deathCountService,
                summaryBuilder,
                compositeRunRecordHook,
                rewardResolverHook,
                foundationContext.timeProvider(),
                logger
        );

        logger.info("Boss engine bootstrap completed. entry_rules=" + entryRuleRegistry.all().size()
                + ", patterns=" + patternRegistry.all().size());
        return Result.success(new BossEngineRuntime(
                runService,
                entryRuleRegistry,
                patternRegistry,
                runRecordHook,
                bossSessionRepository
        ));
    }

    private static Result<Void> validate(
            MasterRegistryContext masterRegistryContext,
            BossEntryRuleRegistry entryRuleRegistry,
            BossPatternRegistry patternRegistry,
            DomainLogger logger
    ) {
        for (BossEntryRule rule : entryRuleRegistry.all().values()) {
            if (masterRegistryContext.bossMasters().find(rule.bossId()).isEmpty()) {
                return Result.failure(
                        ErrorCode.MASTER_SEED_VALIDATION_FAILED,
                        "boss_entry_rule references unknown boss_id: " + rule.bossId()
                );
            }
            if (rule.dailyLimitOpt().isPresent() || rule.weeklyLimitOpt().isPresent()) {
                logger.warn("boss_entry_rule has daily/weekly limits configured but limits are intentionally open. boss_id=" + rule.bossId());
            }
        }

        for (BossPattern pattern : patternRegistry.all().values()) {
            if (masterRegistryContext.bossMasters().find(pattern.bossId()).isEmpty()) {
                return Result.failure(
                        ErrorCode.MASTER_SEED_VALIDATION_FAILED,
                        "boss_pattern_seed references unknown boss_id: " + pattern.bossId() + ", pattern_id=" + pattern.patternId()
                );
            }
            if (pattern.phaseNo() < 0) {
                return Result.failure(
                        ErrorCode.MASTER_SEED_VALIDATION_FAILED,
                        "phase_no must be >= 0. boss_id=" + pattern.bossId() + ", pattern_id=" + pattern.patternId()
                );
            }

            validateBranch(patternRegistry, pattern, pattern.successBranchPatternId(), "success_branch_pattern_id", logger);
            validateBranch(patternRegistry, pattern, pattern.failureBranchPatternId(), "failure_branch_pattern_id", logger);
        }

        return Result.success();
    }

    private static void validateBranch(
            BossPatternRegistry patternRegistry,
            BossPattern sourcePattern,
            String branchPatternId,
            String branchColumn,
            DomainLogger logger
    ) {
        String normalized = normalize(branchPatternId);
        if (normalized.isBlank() || "-".equals(normalized)) {
            return;
        }
        if ("boss_clear".equals(normalized) || "battle_fail".equals(normalized)) {
            return;
        }
        if (patternRegistry.find(sourcePattern.bossId(), normalized).isEmpty()) {
            logger.warn("Unknown branch target in boss_pattern_seed. boss_id=" + sourcePattern.bossId()
                    + ", pattern_id=" + sourcePattern.patternId()
                    + ", " + branchColumn + "=" + branchPatternId);
        }
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
