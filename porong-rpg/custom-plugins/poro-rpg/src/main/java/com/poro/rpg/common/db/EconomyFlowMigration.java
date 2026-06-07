package com.poro.rpg.common.db;

import com.poro.rpg.common.logging.DomainLogger;
import com.poro.rpg.common.result.ErrorCode;
import com.poro.rpg.common.result.Result;

import java.sql.Connection;
import java.sql.Statement;
import java.util.Objects;

public final class EconomyFlowMigration implements MigrationEntryPoint {
    private final DomainLogger logger;

    public EconomyFlowMigration(DomainLogger logger) {
        this.logger = Objects.requireNonNull(logger, "logger");
    }

    @Override
    public Result<Void> migrate(Connection connection) {
        try (Statement st = connection.createStatement()) {
            st.execute(EconomyFlowDdl.CREATE_ECONOMY_FLOW);
            st.execute(EconomyFlowDdl.CREATE_INDEX_ECON_CURRENCY_DATE);
            logger.info("economy_flow DDL applied (idempotent).");
            return Result.success();
        } catch (Exception e) {
            return Result.failure(ErrorCode.DB_CONNECTION_FAILED,
                    "Failed to apply economy_flow DDL", e);
        }
    }
}
