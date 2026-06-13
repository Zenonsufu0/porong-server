package com.poro.rpg.operations.query;

import com.poro.rpg.boss.db.BossSessionRepository;
import com.poro.rpg.boss.engine.BossEngineRuntime;
import com.poro.rpg.common.config.FoundationContext;
import com.poro.rpg.common.logging.DomainLogger;
import com.poro.rpg.common.registry.master.MasterRegistryContext;
import com.poro.rpg.common.result.Result;
import com.poro.rpg.growth.engine.GrowthEngineRuntime;
import com.poro.rpg.life.engine.LifeEngineRuntime;
import com.poro.rpg.operations.http.ActivityApiHandler;
import com.poro.rpg.operations.http.BossApiHandler;
import com.poro.rpg.operations.http.EconomyApiHandler;
import com.poro.rpg.operations.http.PoroHttpServer;
import com.poro.rpg.operations.http.PlayerApiHandler;
import com.poro.rpg.operations.query.api.AdminApiEndpointLayer;
import com.poro.rpg.operations.query.discord.DiscordCommandQueryAdapter;
import com.poro.rpg.operations.query.service.AdminDashboardService;
import com.poro.rpg.operations.query.service.BossStatisticsQueryService;
import com.poro.rpg.operations.query.service.HallOfFameQueryService;
import com.poro.rpg.operations.query.service.LifeStatisticsQueryService;
import com.poro.rpg.operations.query.service.MarketStatisticsQueryService;
import com.poro.rpg.operations.query.service.PlayerDetailQueryService;
import com.poro.rpg.operations.query.service.PublicSnapshotQueryService;
import com.poro.rpg.operations.query.store.InMemoryOperationsDataStore;
import com.poro.rpg.quest.engine.QuestAchievementRuntime;
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

        // 디스코드 인증 (DL-138): 인게임 /인증 발급 → 봇 POST /auth/verify 검증. 코드 TTL 5분.
        com.poro.rpg.auth.AuthService authService = new com.poro.rpg.auth.AuthService(
                new com.poro.rpg.auth.AuthRepository(
                        foundationContext.connectionProvider(), foundationContext.logger().domain("auth")),
                foundationContext.timeProvider(),
                5L * 60L * 1000L);

        PoroHttpServer httpServer;
        try {
            httpServer = PoroHttpServer.create(
                    new BossApiHandler(bossSessionRepository, apiSecretKey),
                    new PlayerApiHandler(publicSnapshotQueryService, apiSecretKey),
                    new ActivityApiHandler(
                            new com.poro.rpg.persistence.PlayerSessionRepository(
                                    foundationContext.connectionProvider(), httpLogger),
                            apiSecretKey),
                    new EconomyApiHandler(
                            new com.poro.rpg.growth.engine.DbEnhancementLogHook(
                                    foundationContext.connectionProvider(), httpLogger),
                            new com.poro.rpg.persistence.EconomyFlowRepository(
                                    foundationContext.connectionProvider(), httpLogger),
                            apiSecretKey),
                    new com.poro.rpg.operations.http.PvpApiHandler(
                            new com.poro.rpg.pvp.db.PvpMatchLogRepository(
                                    foundationContext.connectionProvider(), httpLogger),
                            apiSecretKey),
                    new com.poro.rpg.operations.http.GrowthApiHandler(
                            new com.poro.rpg.persistence.GrowthSnapshotRepository(
                                    foundationContext.connectionProvider(), httpLogger),
                            apiSecretKey),
                    new com.poro.rpg.operations.http.AuthApiHandler(authService, apiSecretKey),
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
                httpServer,
                authService
        ));
    }
}
