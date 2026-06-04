package com.poro.rpg.growth.engine;

import com.poro.rpg.common.config.FoundationContext;
import com.poro.rpg.common.logging.DomainLogger;
import com.poro.rpg.common.registry.RegistryBootstrapper;
import com.poro.rpg.common.registry.master.MasterRegistryContext;
import com.poro.rpg.common.result.ErrorCode;
import com.poro.rpg.common.result.Result;
import com.poro.rpg.common.seed.CsvSeedLoader;
import org.bukkit.plugin.java.JavaPlugin;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class GrowthEngineBootstrap {
    private GrowthEngineBootstrap() {
    }

    public static Result<GrowthEngineRuntime> bootstrap(
            JavaPlugin plugin,
            FoundationContext foundationContext,
            MasterRegistryContext masterRegistryContext
    ) {
        DomainLogger logger = foundationContext.logger().domain("growth-engine");

        Result<Void> installResult = DefaultGrowthSeedInstaller.install(plugin, logger);
        if (installResult.isFailure()) {
            return Result.failure(installResult.errorCode(), installResult.message(), installResult.cause());
        }

        EnhancementRuleRegistry enhancementRuleRegistry = new EnhancementRuleRegistry();
        PotentialOptionRegistry potentialOptionRegistry = new PotentialOptionRegistry();
        // 흔적 전용 슬롯-무관 세부스탯 풀 — 메인 풀과 분리해 전승 슬롯 롤에 오염되지 않게 한다 (DL-129 추가#38).
        PotentialOptionRegistry traceSubstatRegistry = new PotentialOptionRegistry();
        SetBonusRegistry setBonusRegistry = new SetBonusRegistry();
        RuneRegistry runeRegistry = new RuneRegistry();
        EngravingRegistry engravingRegistry = new EngravingRegistry();

        Path seedPath = foundationContext.config().seedPath();
        Result<Void> loadResult = foundationContext.registryBootstrapper().bootstrap(List.of(
                RegistryBootstrapper.task(
                        "growth_enhancement_table",
                        new CsvSeedLoader<>("growth_enhancement_table", seedPath.resolve("growth_enhancement_table.csv"), GrowthSeedMappers::enhancementRule),
                        list -> list.forEach(enhancementRuleRegistry::register)
                ),
                RegistryBootstrapper.task(
                        "growth_potential_option_pool",
                        new CsvSeedLoader<>("growth_potential_option_pool", seedPath.resolve("growth_potential_option_pool.csv"), GrowthSeedMappers::potentialOption),
                        list -> list.forEach(potentialOptionRegistry::register)
                ),
                RegistryBootstrapper.task(
                        "growth_trace_substat_pool",
                        new CsvSeedLoader<>("growth_trace_substat_pool", seedPath.resolve("growth_trace_substat_pool.csv"), GrowthSeedMappers::potentialOption),
                        list -> list.forEach(traceSubstatRegistry::register)
                ),
                RegistryBootstrapper.task(
                        "growth_set_bonus",
                        new CsvSeedLoader<>("growth_set_bonus", seedPath.resolve("growth_set_bonus.csv"), GrowthSeedMappers::setBonusRule),
                        list -> list.forEach(setBonusRegistry::register)
                ),
                RegistryBootstrapper.task(
                        "growth_rune_master",
                        new CsvSeedLoader<>("growth_rune_master", seedPath.resolve("growth_rune_master.csv"), GrowthSeedMappers::runeMaster),
                        list -> list.forEach(runeRegistry::register)
                ),
                RegistryBootstrapper.task(
                        "growth_engraving_master",
                        new CsvSeedLoader<>("growth_engraving_master", seedPath.resolve("growth_engraving_master.csv"), GrowthSeedMappers::engravingMaster),
                        list -> list.forEach(engravingRegistry::register)
                )
        ));
        if (loadResult.isFailure()) {
            return Result.failure(loadResult.errorCode(), loadResult.message(), loadResult.cause());
        }

        Result<Void> validationResult = validate(masterRegistryContext, enhancementRuleRegistry, setBonusRegistry, engravingRegistry, logger);
        if (validationResult.isFailure()) {
            return Result.failure(validationResult.errorCode(), validationResult.message(), validationResult.cause());
        }

        EquipmentService equipmentService = new EquipmentService(
                masterRegistryContext.itemMasters(),
                new NoopStatRecalculationHook()
        );
        // in-memory(관리자 GUI 최근 조회) + DB(누적 분석) 이중 로그 — 합성 hook으로 둘 다 기록 (DL-079)
        InMemoryEnhancementLogHook enhancementLogHook = new InMemoryEnhancementLogHook();
        DbEnhancementLogHook dbEnhancementLogHook = new DbEnhancementLogHook(
                foundationContext.connectionProvider(),
                foundationContext.logger().domain("db.enhancement"));
        EnhancementLogHook enhancementLogSink = new CompositeEnhancementLogHook(
                List.of(enhancementLogHook, dbEnhancementLogHook));
        EnhancementService enhancementService = new EnhancementService(
                masterRegistryContext.itemMasters(),
                enhancementRuleRegistry,
                enhancementLogSink,
                new ThreadLocalRandomProvider()
        );
        PotentialService potentialService = new PotentialService(
                masterRegistryContext.itemMasters(),
                potentialOptionRegistry,
                new PreferAfterPotentialSelectionHook(),
                new ThreadLocalRandomProvider()
        );
        SuccessionService successionService = new SuccessionService(potentialOptionRegistry, masterRegistryContext.itemMasters(), new ThreadLocalRandomProvider());
        TraceSubstatRoller traceSubstatRoller = new TraceSubstatRoller(traceSubstatRegistry, new ThreadLocalRandomProvider());
        SetBonusService setBonusService = new SetBonusService(masterRegistryContext.itemMasters(), setBonusRegistry);
        RuneService runeService = new RuneService(runeRegistry, masterRegistryContext.itemMasters());
        EngravingService engravingService = new EngravingService(engravingRegistry);
        PlayerGrowthSnapshotBuilder snapshotBuilder = new PlayerGrowthSnapshotBuilder(
                masterRegistryContext.itemMasters(),
                setBonusService,
                runeService,
                engravingService,
                potentialService
        );

        logger.info("Growth engine bootstrap completed. enhancement_rules=" + enhancementRuleRegistry.all().size()
                + ", potential_options=" + potentialOptionRegistry.all().size()
                + ", trace_substat_options=" + traceSubstatRegistry.all().size()
                + ", set_bonus_rules=" + setBonusRegistry.all().size()
                + ", runes=" + runeRegistry.all().size()
                + ", engravings=" + engravingRegistry.all().size());

        return Result.success(new GrowthEngineRuntime(
                equipmentService,
                enhancementService,
                potentialService,
                successionService,
                setBonusService,
                runeService,
                engravingService,
                snapshotBuilder,
                enhancementRuleRegistry,
                potentialOptionRegistry,
                setBonusRegistry,
                runeRegistry,
                engravingRegistry,
                enhancementLogHook,
                traceSubstatRoller
        ));
    }

    private static Result<Void> validate(
            MasterRegistryContext masterRegistryContext,
            EnhancementRuleRegistry enhancementRuleRegistry,
            SetBonusRegistry setBonusRegistry,
            EngravingRegistry engravingRegistry,
            DomainLogger logger
    ) {
        List<String> blockingErrors = new ArrayList<>();

        for (GrowthTier tier : GrowthTier.values()) {
            for (int level = 1; level <= EnhancementService.MAX_ENHANCE_LEVEL; level++) {
                EnhancementRule rule = enhancementRuleRegistry.find(tier, level).orElse(null);
                if (rule == null) {
                    blockingErrors.add("Missing enhancement rule: tier=" + tier + ", level=" + level);
                    continue;
                }
                // 확정 강화표(economy_numbers_v2): 1~3강 100%, 4강 95%·5강 90%부터 하향. 검증은 1~3강만 100% 보장.
                if (level <= 3 && rule.successRate() != 100.0d) {
                    blockingErrors.add("Enhancement level 1~3 must be 100%. tier=" + tier + ", level=" + level + ", actual=" + rule.successRate());
                }
                if (rule.breakOnFail() || rule.downgradeOnFail()) {
                    blockingErrors.add("Enhancement fail policy must not break/downgrade. tier=" + tier + ", level=" + level);
                }
            }
        }

        Map<String, List<SetBonusRule>> rulesBySet = new java.util.LinkedHashMap<>();
        for (SetBonusRule rule : setBonusRegistry.all().values()) {
            rulesBySet.computeIfAbsent(rule.setId(), ignored -> new ArrayList<>()).add(rule);
        }
        for (Map.Entry<String, List<SetBonusRule>> entry : rulesBySet.entrySet()) {
            java.util.Set<Integer> pieces = entry.getValue().stream().map(SetBonusRule::pieceCount).collect(java.util.stream.Collectors.toSet());
            if (!pieces.contains(3) || !pieces.contains(4) || !pieces.contains(7)) {
                logger.warn("Set bonus does not fully contain 3/4/7 pieces: set_id=" + entry.getKey());
            }
        }

        for (EngravingMaster engraving : engravingRegistry.all().values()) {
            if (engraving.classType() && engraving.maxLevel() != 1) {
                logger.warn("Class engraving max_level should be 1. engraving_id=" + engraving.engravingId());
            }
        }

        for (com.poro.rpg.common.registry.master.model.ItemMaster item : masterRegistryContext.itemMasters().all().values()) {
            if (GrowthTier.from(item.tier()) == GrowthTier.T2 && (item.setId() == null || item.setId().isBlank())) {
                logger.warn("T2 item without set_id in growth context: item_id=" + item.itemId());
            }
        }

        if (!blockingErrors.isEmpty()) {
            for (String error : blockingErrors) {
                logger.error(error);
            }
            return Result.failure(
                    ErrorCode.MASTER_SEED_VALIDATION_FAILED,
                    "Growth seed validation failed with " + blockingErrors.size() + " blocking errors."
            );
        }

        return Result.success();
    }
}
