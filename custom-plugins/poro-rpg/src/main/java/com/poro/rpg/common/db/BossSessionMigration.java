package com.poro.rpg.common.db;

import com.poro.rpg.common.logging.DomainLogger;
import com.poro.rpg.common.result.ErrorCode;
import com.poro.rpg.common.result.Result;

import java.sql.Connection;
import java.sql.Statement;
import java.util.Objects;

public final class BossSessionMigration implements MigrationEntryPoint {
    private final DomainLogger logger;

    public BossSessionMigration(DomainLogger logger) {
        this.logger = Objects.requireNonNull(logger, "logger");
    }

    @Override
    public Result<Void> migrate(Connection connection) {
        try (Statement st = connection.createStatement()) {
            st.execute(BossSessionDdl.CREATE_SESSION_LOG);
            st.execute(BossSessionDdl.CREATE_INDEX_BOSS_WEEK);
            st.execute(BossSessionDdl.CREATE_INDEX_RESULT);
            st.execute(BossSessionDdl.CREATE_SESSION_PLAYER);
            st.execute(BossSessionDdl.CREATE_INDEX_SESSION);
            st.execute(BossSessionDdl.CREATE_STATS_SUMMARY_VIEW);
            logger.info("boss_session_log + boss_session_player DDL applied (idempotent).");
            return Result.success();
        } catch (Exception e) {
            return Result.failure(ErrorCode.DB_CONNECTION_FAILED,
                    "Failed to apply boss_session DDL", e);
        }
    }
}
