package com.poro.empire.common.db;

import com.poro.empire.common.logging.DomainLogger;
import com.poro.empire.common.result.ErrorCode;
import com.poro.empire.common.result.Result;

import java.sql.Connection;
import java.sql.Statement;
import java.util.Objects;

public final class EnhancementLogMigration implements MigrationEntryPoint {
    private final DomainLogger logger;

    public EnhancementLogMigration(DomainLogger logger) {
        this.logger = Objects.requireNonNull(logger, "logger");
    }

    @Override
    public Result<Void> migrate(Connection connection) {
        try (Statement st = connection.createStatement()) {
            st.execute(EnhancementLogDdl.CREATE_ENHANCEMENT_LOG);
            st.execute(EnhancementLogDdl.CREATE_INDEX_ENH_TIER_TARGET);
            st.execute(EnhancementLogDdl.CREATE_INDEX_ENH_PLAYER);
            logger.info("enhancement_log DDL applied (idempotent).");
            return Result.success();
        } catch (Exception e) {
            return Result.failure(ErrorCode.DB_CONNECTION_FAILED,
                    "Failed to apply enhancement_log DDL", e);
        }
    }
}
