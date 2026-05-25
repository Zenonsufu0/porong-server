package com.poro.empire.operations.query;

import com.poro.empire.boss.db.BossSessionRepository;
import com.poro.empire.boss.engine.BossEngineRuntime;
import com.poro.empire.common.config.FoundationContext;
import com.poro.empire.common.logging.DomainLogger;
import com.poro.empire.common.registry.master.MasterRegistryContext;
import com.poro.empire.common.result.Result;
import com.poro.empire.growth.engine.GrowthEngineRuntime;
import com.poro.empire.life.engine.LifeEngineRuntime;
import com.poro.empire.operations.http.BossApiHandler;
import com.poro.empire.operations.http.EmpireHttpServer;
import com.poro.empire.operations.query.api.AdminApiEndpointLayer;
import com.poro.empire.operations.query.discord.DiscordCommandQueryAdapter;
import com.poro.empire.operations.query.service.AdminDashboardService;
import com.poro.empire.operations.query.service.BossStatisticsQueryService;
import com.poro.empire.operations.query.service.HallOfFameQueryService;
import com.poro.empire.operations.query.service.LifeStatisticsQueryService;
import com.poro.empire.operations.query.service.MarketStatisticsQueryService;
import com.poro.empire.operations.query.service.PlayerDetailQueryService;
import com.poro.empire.operations.query.service.PublicSnapshotQueryService;
import com.poro.empire.operations.query.store.InMemoryOperationsDataStore;
import com.poro.empire.quest.engine.QuestAchievementRuntime;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

public final class OperationsQueryBootstrap {
    private OperationsQueryBootstrap() {
    }

    public static Result<OperationsQueryRuntime> bootstrap(
            JavaPlugin plugin,
            FoundationContext foundationContext,
            MasterRegistryContext masterRegistryContext,
            BossEngineRuntime bossEngineRuntime,
            GrowthEngineRuntime growthEngineRuntime,
            LifeEngineRuntime lifeEngineRuntime,
            QuestAchievementRuntime questAchievementRuntime
    ) {
        Objects.requireNonNull(plugin, "plugin");
        Objects.requireNonNull(foundationContext, "foundationContext");
        Objects.requireNonNull(masterRegistryContext, "masterRegistryContext");
        Objects.requireNonNull(bossEngineRuntime, "bossEngineRuntime");
        Objects.requireNonNull(growthEngineRuntime, "growthEngineRuntime");
        Objects.requireNonNull(lifeEngineRuntime, "lifeEngineRuntime");
        Objects.requireNonNull(questAchievementRuntime, "questAchievementRuntime");

        DomainLogger logger = foundationContext.logger().domain("operations-query");
        InMemoryOperationsDataStore dataStore = new InMemoryOperationsDataStore();
        BossSessionRepository bossSessionRepository = bossEngineRuntime.bossSessionRepository();

        BossStatisticsQueryService bossStatisticsQueryService = new BossStatisticsQueryService(
                masterRegistryContext.bossMasters(),
                bossEngineRuntime,
                dataStore
        );
        MarketStatisticsQueryService marketStatisticsQueryService = new MarketStatisticsQueryService(dataStore);
        LifeStatisticsQueryService lifeStatisticsQueryService = new LifeStatisticsQueryService(lifeEngineRuntime, dataStore);
        HallOfFameQueryService hallOfFameQueryService = new HallOfFameQueryService(
                questAchievementRuntime,
                bossEngineRuntime,
                dataStore
        );
        PlayerDetailQueryService playerDetailQueryService = new PlayerDetailQueryService(dataStore, bossEngineRuntime);
        PublicSnapshotQueryService publicSnapshotQueryService = new PublicSnapshotQueryService(
                playerDetailQueryService,
                marketStatisticsQueryService,
                hallOfFameQueryService
        );
        AdminDashboardService adminDashboardService = new AdminDashboardService(
                bossStatisticsQueryService,
                marketStatisticsQueryService,
                lifeStatisticsQueryService,
                dataStore
        );
        AdminApiEndpointLayer adminApiEndpointLayer = new AdminApiEndpointLayer(
                adminDashboardService,
                playerDetailQueryService
        );
        DiscordCommandQueryAdapter discordCommandQueryAdapter = new DiscordCommandQueryAdapter(publicSnapshotQueryService);

        String apiSecretKey = foundationContext.config().apiSecretKey();
        String apiBind = foundationContext.config().apiBind();
        DomainLogger httpLogger = foundationContext.logger().domain("http");
        if (apiSecretKey.isBlank()) {
            httpLogger.warn("common.api-secret-key not configured — HTTP API will reject all requests with 503.");
        }

        EmpireHttpServer httpServer;
        try {
            httpServer = EmpireHttpServer.create(
                    new BossApiHandler(bossSessionRepository, apiSecretKey),
                    apiBind,
                    httpLogger
            );
            httpServer.start();
        } catch (Exception e) {
            logger.warn("Failed to start HTTP server: " + e.getMessage() + " — API disabled.");
            httpServer = null;
        }

        logger.info("Operations/query bootstrap completed. endpoints="
                + adminApiEndpointLayer.listEndpoints().getOrDefault("GET", java.util.List.of()).toString());

        return Result.success(new OperationsQueryRuntime(
                dataStore,
                adminDashboardService,
                playerDetailQueryService,
                bossStatisticsQueryService,
                marketStatisticsQueryService,
                lifeStatisticsQueryService,
                hallOfFameQueryService,
                publicSnapshotQueryService,
                adminApiEndpointLayer,
                discordCommandQueryAdapter,
                bossSessionRepository,
                httpServer
        ));
    }
}
