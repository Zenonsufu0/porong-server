package com.poro.empire.common.config;

import com.poro.empire.common.db.AuctionMigration;
import com.poro.empire.common.db.BossSessionMigration;
import com.poro.empire.common.db.BossSessionMigrationV2;
import com.poro.empire.common.db.CompositeMigrationEntryPoint;
import com.poro.empire.common.db.IslandSettingsMigration;
import com.poro.empire.common.db.PvpMigration;
import com.poro.empire.common.db.PvpMatchLogMigrationV2;
import com.poro.empire.common.db.PlayerSessionMigration;
import com.poro.empire.common.db.EnhancementLogMigration;
import com.poro.empire.common.db.EconomyFlowMigration;
import com.poro.empire.common.db.ConnectionProvider;
import com.poro.empire.common.db.DatabaseBootstrapper;
import com.poro.empire.common.db.JdbcTransactionHelper;
import com.poro.empire.common.db.MigrationEntryPoint;
import com.poro.empire.common.db.PlayerFlagTableStubMigration;
import com.poro.empire.common.db.SqliteConnectionProvider;
import com.poro.empire.common.db.TransactionHelper;

import java.util.List;
import com.poro.empire.common.logging.CommonPluginLogger;
import com.poro.empire.common.logging.CommonPluginLoggerFactory;
import com.poro.empire.common.logging.DomainLogger;
import com.poro.empire.common.registry.RegistryBootstrapper;
import com.poro.empire.common.result.ErrorCode;
import com.poro.empire.common.result.Result;
import com.poro.empire.common.time.SystemTimeProvider;
import com.poro.empire.common.time.TimeProvider;
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
                    new AuctionMigration(logger.domain("db.migration.auction")),
                    new PvpMigration(logger.domain("db.migration.pvp")),
                    new PvpMatchLogMigrationV2(logger.domain("db.migration.pvp-v2")),
                    new IslandSettingsMigration(logger.domain("db.migration.island-settings")),
                    new PlayerSessionMigration(logger.domain("db.migration.player-session")),
                    new EnhancementLogMigration(logger.domain("db.migration.enhancement-log")),
                    new EconomyFlowMigration(logger.domain("db.migration.economy-flow"))
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
