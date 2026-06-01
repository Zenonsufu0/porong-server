package com.poro.rpg.common.db;

import com.poro.rpg.common.logging.DomainLogger;
import com.poro.rpg.common.result.ErrorCode;
import com.poro.rpg.common.result.Result;

import java.sql.Connection;
import java.util.Objects;

public final class DatabaseBootstrapper {
    private final ConnectionProvider connectionProvider;
    private final MigrationEntryPoint migrationEntryPoint;
    private final DomainLogger logger;

    public DatabaseBootstrapper(
            ConnectionProvider connectionProvider,
            MigrationEntryPoint migrationEntryPoint,
            DomainLogger logger
    ) {
        this.connectionProvider = Objects.requireNonNull(connectionProvider, "connectionProvider");
        this.migrationEntryPoint = Objects.requireNonNull(migrationEntryPoint, "migrationEntryPoint");
        this.logger = Objects.requireNonNull(logger, "logger");
    }

    public Result<Void> initialize() {
        Result<Connection> connectionResult = connectionProvider.getConnection();
        if (connectionResult.isFailure()) {
            return Result.failure(connectionResult.errorCode(), connectionResult.message(), connectionResult.cause());
        }

        try (Connection connection = connectionResult.value()) {
            Result<Void> migrationResult = migrationEntryPoint.migrate(connection);
            if (migrationResult.isFailure()) {
                return Result.failure(
                        ErrorCode.DB_CONNECTION_FAILED,
                        migrationResult.message(),
                        migrationResult.cause()
                );
            }
            logger.info("Database bootstrap completed.");
            return Result.success();
        } catch (Exception exception) {
            return Result.failure(
                    ErrorCode.DB_CONNECTION_FAILED,
                    "Failed to bootstrap database",
                    exception
            );
        }
    }
}
