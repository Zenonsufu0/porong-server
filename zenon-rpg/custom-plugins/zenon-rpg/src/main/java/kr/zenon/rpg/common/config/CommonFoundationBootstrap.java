package kr.zenon.rpg.common.config;

import kr.zenon.rpg.common.db.AuctionMigration;
import kr.zenon.rpg.common.db.AuthMigration;
import kr.zenon.rpg.common.db.BossSessionMigration;
import kr.zenon.rpg.common.db.BossSessionMigrationV2;
import kr.zenon.rpg.common.db.BossSessionPlayerMigrationV3;
import kr.zenon.rpg.common.db.CompositeMigrationEntryPoint;
import kr.zenon.rpg.common.db.IslandSettingsMigration;
import kr.zenon.rpg.common.db.PvpMigration;
import kr.zenon.rpg.common.db.PvpMatchLogMigrationV2;
import kr.zenon.rpg.common.db.PlayerSessionMigration;
import kr.zenon.rpg.common.db.EnhancementLogMigration;
import kr.zenon.rpg.common.db.EconomyFlowMigration;
import kr.zenon.rpg.common.db.GrowthSnapshotMigration;
import kr.zenon.rpg.common.db.ConnectionProvider;
import kr.zenon.rpg.common.db.DatabaseBootstrapper;
import kr.zenon.rpg.common.db.JdbcTransactionHelper;
import kr.zenon.rpg.common.db.MigrationEntryPoint;
import kr.zenon.rpg.common.db.PlayerFlagTableStubMigration;
import kr.zenon.rpg.common.db.RuntimeConfigMigration;
import kr.zenon.rpg.common.db.SqliteConnectionProvider;
import kr.zenon.rpg.common.db.TransactionHelper;

import java.util.List;
import kr.zenon.rpg.common.logging.CommonPluginLogger;
import kr.zenon.rpg.common.logging.CommonPluginLoggerFactory;
import kr.zenon.rpg.common.logging.DomainLogger;
import kr.zenon.rpg.common.registry.RegistryBootstrapper;
import kr.zenon.rpg.common.result.ErrorCode;
import kr.zenon.rpg.common.result.Result;
import kr.zenon.rpg.common.time.SystemTimeProvider;
import kr.zenon.rpg.common.time.TimeProvider;
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
