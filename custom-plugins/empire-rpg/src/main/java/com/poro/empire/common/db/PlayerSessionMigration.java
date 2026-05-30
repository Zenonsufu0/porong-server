package com.poro.empire.common.db;

import com.poro.empire.common.logging.DomainLogger;
import com.poro.empire.common.result.ErrorCode;
import com.poro.empire.common.result.Result;

import java.sql.Connection;
import java.sql.Statement;
import java.util.Objects;

public final class PlayerSessionMigration implements MigrationEntryPoint {
    private final DomainLogger logger;

    public PlayerSessionMigration(DomainLogger logger) {
        this.logger = Objects.requireNonNull(logger, "logger");
    }

    @Override
    public Result<Void> migrate(Connection connection) {
        try (Statement st = connection.createStatement()) {
            st.execute(PlayerSessionDdl.CREATE_PLAYER_SESSION_LOG);
            st.execute(PlayerSessionDdl.CREATE_INDEX_SESSION_UUID);
            st.execute(PlayerSessionDdl.CREATE_INDEX_SESSION_DATE);
            st.execute(PlayerSessionDdl.CREATE_INDEX_SESSION_OPEN);
            logger.info("player_session_log DDL applied (idempotent).");
            return Result.success();
        } catch (Exception e) {
            return Result.failure(ErrorCode.DB_CONNECTION_FAILED,
                    "Failed to apply player_session DDL", e);
        }
    }
}
