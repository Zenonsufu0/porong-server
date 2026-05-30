package com.poro.empire.common.db;

import com.poro.empire.common.logging.DomainLogger;
import com.poro.empire.common.result.ErrorCode;
import com.poro.empire.common.result.Result;

import java.sql.Connection;
import java.sql.Statement;
import java.util.Objects;

public final class GrowthSnapshotMigration implements MigrationEntryPoint {
    private final DomainLogger logger;

    public GrowthSnapshotMigration(DomainLogger logger) {
        this.logger = Objects.requireNonNull(logger, "logger");
    }

    @Override
    public Result<Void> migrate(Connection connection) {
        try (Statement st = connection.createStatement()) {
            st.execute(GrowthSnapshotDdl.CREATE_GROWTH_SNAPSHOT);
            st.execute(GrowthSnapshotDdl.CREATE_INDEX_SNAP_DATE);
            logger.info("growth_snapshot DDL applied (idempotent).");
            return Result.success();
        } catch (Exception e) {
            return Result.failure(ErrorCode.DB_CONNECTION_FAILED,
                    "Failed to apply growth_snapshot DDL", e);
        }
    }
}
