package com.poro.empire.common.db;

import com.poro.empire.common.logging.DomainLogger;
import com.poro.empire.common.result.ErrorCode;
import com.poro.empire.common.result.Result;

import java.sql.Connection;
import java.sql.Statement;
import java.util.Objects;

public final class PvpMigration implements MigrationEntryPoint {
    private final DomainLogger logger;

    public PvpMigration(DomainLogger logger) {
        this.logger = Objects.requireNonNull(logger, "logger");
    }

    @Override
    public Result<Void> migrate(Connection connection) {
        try (Statement st = connection.createStatement()) {
            st.execute(PvpDdl.CREATE_PVP_RATING);
            st.execute(PvpDdl.CREATE_INDEX_RATING_SCORE);
            st.execute(PvpDdl.CREATE_PVP_MATCH_LOG);
            st.execute(PvpDdl.CREATE_INDEX_MATCH_TYPE);
            logger.info("pvp_rating + pvp_match_log DDL applied (idempotent).");
            return Result.success();
        } catch (Exception e) {
            return Result.failure(ErrorCode.DB_CONNECTION_FAILED,
                    "Failed to apply pvp DDL", e);
        }
    }
}
