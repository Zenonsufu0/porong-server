package com.poro.rpg.common.db;

import com.poro.rpg.common.logging.DomainLogger;
import com.poro.rpg.common.result.ErrorCode;
import com.poro.rpg.common.result.Result;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Objects;

public final class SqliteConnectionProvider implements ConnectionProvider {
    private final String jdbcUrl;
    private final DomainLogger logger;

    public SqliteConnectionProvider(String jdbcUrl, DomainLogger logger) {
        this.jdbcUrl = Objects.requireNonNull(jdbcUrl, "jdbcUrl");
        this.logger = Objects.requireNonNull(logger, "logger");
    }

    @Override
    public Result<Connection> getConnection() {
        try {
            return Result.success(DriverManager.getConnection(jdbcUrl));
        } catch (SQLException exception) {
            logger.error("Failed to open SQLite connection: " + jdbcUrl, exception);
            return Result.failure(
                    ErrorCode.DB_CONNECTION_FAILED,
                    "Failed to open SQLite connection: " + jdbcUrl,
                    exception
            );
        }
    }
}
