package com.poro.rpg.common.config;

import com.poro.rpg.common.db.AuctionMigration;
import com.poro.rpg.common.db.AuthMigration;
import com.poro.rpg.common.db.BossSessionMigration;
import com.poro.rpg.common.db.BossSessionMigrationV2;
import com.poro.rpg.common.db.BossSessionPlayerMigrationV3;
import com.poro.rpg.common.db.CompositeMigrationEntryPoint;
import com.poro.rpg.common.db.IslandSettingsMigration;
import com.poro.rpg.common.db.PvpMigration;
import com.poro.rpg.common.db.PvpMatchLogMigrationV2;
import com.poro.rpg.common.db.PlayerSessionMigration;
import com.poro.rpg.common.db.EnhancementLogMigration;
import com.poro.rpg.common.db.EconomyFlowMigration;
import com.poro.rpg.common.db.GrowthSnapshotMigration;
import com.poro.rpg.common.db.ConnectionProvider;
import com.poro.rpg.common.db.DatabaseBootstrapper;
import com.poro.rpg.common.db.JdbcTransactionHelper;
import com.poro.rpg.common.db.MigrationEntryPoint;
import com.poro.rpg.common.db.PlayerFlagTableStubMigration;
import com.poro.rpg.common.db.RuntimeConfigMigration;
import com.poro.rpg.common.db.SqliteConnectionProvider;
import com.poro.rpg.common.db.TransactionHelper;

import java.util.List;
import com.poro.rpg.common.logging.CommonPluginLogger;
import com.poro.rpg.common.logging.CommonPluginLoggerFactory;
import com.poro.rpg.common.logging.DomainLogger;
import com.poro.rpg.common.registry.RegistryBootstrapper;
import com.poro.rpg.common.result.ErrorCode;
import com.poro.rpg.common.result.Result;
import com.poro.rpg.common.time.SystemTimeProvider;
import com.poro.rpg.common.time.TimeProvider;
import org.bukkit.plugin.java.JavaPlugin;

public final class CommonFoundationBootstrap {
    private CommonFoundationBootstrap() {
    }

    public static Result<FoundationContext> bootstrap(JavaPlugin plugin) {
        try {
            CommonPluginLogger logger = CommonPluginLoggerFactory.fromPlugin(plugin);
            DomainLogger bootstrapLog = logger.domain("foundation");

            CommonConfig config = CommonConfigLoader.load(plugin);
            TimeProvider timeProvider = SystemTimeProvider.systemUtc();

            ConnectionProvider connectionProvider = new SqliteConnectionProvider(
                    config.sqliteJdbcUrl(),
                    logger.domain("db.connection")
            );
            TransactionHelper transactionHelper = new JdbcTransactionHelper(
                    connectionProvider,
                    logger.domain("db.transaction")
            );
            RegistryBootstrapper registryBootstrapper = new RegistryBootstrapper(logger.domain("registry"));
            MigrationEntryPoint migrationEntryPoint = new CompositeMigrationEntryPoint(List.of(
                    new PlayerFlagTableStubMigration(logger.domain("db.migration.player-flag")),
                    new BossSessionMigration(logger.domain("db.migration.boss-session")),
                    new BossSessionMigrationV2(logger.domain("db.migration.boss-session-v2")),
                    new BossSessionPlayerMigrationV3(logger.domain("db.migration.boss-session-v3")),
                    new AuctionMigration(logger.domain("db.migration.auction")),
                    new AuthMigration(logger.domain("db.migration.auth")),
                    new PvpMigration(logger.domain("db.migration.pvp")),
                    new PvpMatchLogMigrationV2(logger.domain("db.migration.pvp-v2")),
                    new IslandSettingsMigration(logger.domain("db.migration.island-settings")),
                    new PlayerSessionMigration(logger.domain("db.migration.player-session")),
                    new EnhancementLogMigration(logger.domain("db.migration.enhancement-log")),
                    new EconomyFlowMigration(logger.domain("db.migration.economy-flow")),
                    new GrowthSnapshotMigration(logger.domain("db.migration.growth-snapshot")),
                    new RuntimeConfigMigration(logger.domain("db.migration.runtime-config"))
            ));
            DatabaseBootstrapper databaseBootstrapper = new DatabaseBootstrapper(
                    connectionProvider,
                    migrationEntryPoint,
                    logger.domain("db.migration")
            );
            Result<Void> dbInit = databaseBootstrapper.initialize();
            if (dbInit.isFailure()) {
                return Result.failure(dbInit.errorCode(), dbInit.message(), dbInit.cause());
            }

            bootstrapLog.info("Common foundation initialized.");
            return Result.success(new FoundationContext(
                    config,
                    timeProvider,
                    logger,
                    connectionProvider,
                    transactionHelper,
                    registryBootstrapper,
                    databaseBootstrapper
            ));
        } catch (Exception exception) {
            return Result.failure(
                    ErrorCode.UNKNOWN,
                    "Failed to initialize common foundation",
                    exception
            );
        }
    }
}
