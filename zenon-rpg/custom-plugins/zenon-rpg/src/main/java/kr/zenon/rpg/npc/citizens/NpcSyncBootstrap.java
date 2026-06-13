package kr.zenon.rpg.npc.citizens;

import kr.zenon.rpg.common.config.FoundationContext;
import kr.zenon.rpg.common.logging.DomainLogger;
import kr.zenon.rpg.common.registry.master.MasterRegistryContext;
import kr.zenon.rpg.common.result.Result;
import org.bukkit.plugin.java.JavaPlugin;

import java.nio.file.Path;
import java.util.Objects;

public final class NpcSyncBootstrap {
    private NpcSyncBootstrap() {
    }

    public static Result<NpcSyncRuntime> bootstrap(
            JavaPlugin plugin,
            FoundationContext foundationContext,
            MasterRegistryContext masterRegistryContext
    ) {
        Objects.requireNonNull(plugin, "plugin");
        Objects.requireNonNull(foundationContext, "foundationContext");
        Objects.requireNonNull(masterRegistryContext, "masterRegistryContext");

        DomainLogger logger = foundationContext.logger().domain("npc-sync");
        Result<Void> installResult = DefaultNpcSyncSeedInstaller.install(plugin, logger);
        if (installResult.isFailure()) {
            return Result.failure(installResult.errorCode(), installResult.message(), installResult.cause());
        }

        Path seedPath = foundationContext.config().seedPath().resolve("npc_spawn_seed.csv");
        CitizensNpcSeedLoader seedLoader = new CitizensNpcSeedLoader(seedPath);
        CitizensNpcGateway gateway = new ReflectionCitizensNpcGateway(plugin, logger);
        CitizensNpcTraitBinder traitBinder = new CitizensNpcTraitBinder(
                new MetadataInteractionProfileHook(),
                new MetadataBetonQuestConversationHook(plugin)
        );
        CitizensNpcSyncService syncService = new CitizensNpcSyncService(
                logger,
                seedLoader,
                gateway,
                traitBinder,
                masterRegistryContext
        );

        if (!gateway.isAvailable()) {
            logger.warn("Citizens plugin is not enabled. NPC auto-sync bootstrap is registered but skipped.");
            return Result.success(new NpcSyncRuntime(
                    seedLoader,
                    syncService,
                    gateway,
                    new CitizensNpcSyncReport(0, 0, 0, 0, 0, 0, 0, 0, java.util.List.of()),
                    false
            ));
        }

        Result<CitizensNpcSyncReport> syncResult = syncService.sync();
        if (syncResult.isFailure()) {
            return Result.failure(syncResult.errorCode(), syncResult.message(), syncResult.cause());
        }

        if (plugin.getServer().getPluginManager().isPluginEnabled("BetonQuest")) {
            plugin.getServer().getPluginManager().registerEvents(
                    new CitizensBetonQuestConversationListener(plugin, logger, gateway),
                    plugin
            );
            logger.info("Registered Citizens -> BetonQuest conversation click bridge listener.");
        } else {
            logger.warn("BetonQuest is not enabled. Citizens click conversation bridge listener is skipped.");
        }

        CitizensNpcSyncReport report = syncResult.value();
        logger.info("NpcSyncBootstrap completed. created=" + report.createdCount()
                + ", updated=" + report.updatedCount()
                + ", removed=" + report.removedCount()
                + ", orphan=" + report.orphanCount());
        return Result.success(new NpcSyncRuntime(seedLoader, syncService, gateway, report, true));
    }
}
