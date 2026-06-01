package com.poro.rpg.combat.engine;

import com.poro.rpg.common.config.FoundationContext;
import com.poro.rpg.common.logging.DomainLogger;
import com.poro.rpg.common.registry.master.MasterRegistryContext;
import com.poro.rpg.common.registry.master.model.SkillMaster;
import com.poro.rpg.common.result.Result;
import com.poro.rpg.common.seed.CsvSeedLoader;
import com.poro.rpg.common.seed.CsvRow;
import org.bukkit.plugin.java.JavaPlugin;

import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

public final class CombatEngineBootstrap {
    private CombatEngineBootstrap() {
    }

    public static Result<CombatEngineRuntime> bootstrap(
            JavaPlugin plugin,
            FoundationContext foundationContext,
            MasterRegistryContext masterRegistryContext
    ) {
        DomainLogger logger = foundationContext.logger().domain("combat-engine");
        Result<Void> installResult = DefaultCombatSeedInstaller.install(plugin, logger);
        if (installResult.isFailure()) {
            return Result.failure(installResult.errorCode(), installResult.message(), installResult.cause());
        }

        Path stateSeedPath = foundationContext.config().seedPath().resolve("state_master.csv");
        CsvSeedLoader<StateDefinition> stateLoader = new CsvSeedLoader<>(
                "state_master",
                stateSeedPath,
                CombatEngineBootstrap::mapStateRow
        );

        Result<List<StateDefinition>> loadedStates = stateLoader.load();
        if (loadedStates.isFailure()) {
            logger.error("Failed to load state registry seed: " + loadedStates.message(), loadedStates.cause());
            return Result.failure(loadedStates.errorCode(), loadedStates.message(), loadedStates.cause());
        }

        StateRegistry stateRegistry = new StateRegistry();
        loadedStates.value().forEach(stateRegistry::register);

        BuffDebuffService buffDebuffService = new BuffDebuffService(stateRegistry);
        StateApplier stateApplier = new StateApplier(stateRegistry, buffDebuffService);
        ResourceHandler resourceHandler = new ResourceHandler();
        CombatFormulaResolver combatFormulaResolver = new CombatFormulaResolver();
        TagDamageResolver tagDamageResolver = new TagDamageResolver();
        ConditionalDamageResolver conditionalDamageResolver = new ConditionalDamageResolver();
        CombatEngine combatEngine = new CombatEngine();

        validateSkillStateReferences(masterRegistryContext, stateRegistry, logger);

        logger.info("Combat engine bootstrap completed. states=" + stateRegistry.all().size());
        return Result.success(new CombatEngineRuntime(
                combatEngine,
                stateRegistry,
                buffDebuffService,
                stateApplier,
                resourceHandler,
                combatFormulaResolver,
                tagDamageResolver,
                conditionalDamageResolver
        ));
    }

    private static StateDefinition mapStateRow(CsvRow row) {
        return new StateDefinition(
                row.required("state_code"),
                row.required("state_group"),
                row.required("display_name"),
                row.optionalBoolean("is_stackable", false),
                row.optionalInt("max_stack", 1),
                row.optionalDouble("duration_default", 0.0d),
                row.optional("notes")
        );
    }

    private static void validateSkillStateReferences(
            MasterRegistryContext masterRegistryContext,
            StateRegistry stateRegistry,
            DomainLogger logger
    ) {
        for (SkillMaster skill : masterRegistryContext.skillMasters().all().values()) {
            String stateCode = normalize(skill.stateCode());
            if (stateCode.isBlank() || "none".equals(stateCode)) {
                continue;
            }
            if (!stateRegistry.contains(stateCode)) {
                logger.warn("Skill '" + skill.skillId() + "' references unknown state_code '" + skill.stateCode() + "'.");
            }
        }
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
