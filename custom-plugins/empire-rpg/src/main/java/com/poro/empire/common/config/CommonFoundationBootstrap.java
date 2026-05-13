package com.poro.empire.common.config;

import com.poro.empire.common.db.ConnectionProvider;
import com.poro.empire.common.db.DatabaseBootstrapper;
import com.poro.empire.common.db.JdbcTransactionHelper;
import com.poro.empire.common.db.NoopMigrationEntryPoint;
import com.poro.empire.common.db.SqliteConnectionProvider;
import com.poro.empire.common.db.TransactionHelper;
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
            DatabaseBootstrapper databaseBootstrapper = new DatabaseBootstrapper(
                    connectionProvider,
                    new NoopMigrationEntryPoint(),
                    logger.domain("db.migration")
            );

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
