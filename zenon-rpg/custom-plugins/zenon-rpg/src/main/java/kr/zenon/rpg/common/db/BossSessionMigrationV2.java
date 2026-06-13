package kr.zenon.rpg.common.db;

import kr.zenon.rpg.common.logging.DomainLogger;
import kr.zenon.rpg.common.result.ErrorCode;
import kr.zenon.rpg.common.result.Result;

import java.sql.Connection;
import java.sql.Statement;
import java.util.Objects;

/** boss_stats_summary 뷰를 DROP → CREATE 하여 WHERE ended_at IS NOT NULL 조건을 반영한다. */
public final class BossSessionMigrationV2 implements MigrationEntryPoint {
    private final DomainLogger logger;

    public BossSessionMigrationV2(DomainLogger logger) {
        this.logger = Objects.requireNonNull(logger, "logger");
    }

    @Override
    public Result<Void> migrate(Connection connection) {
        try (Statement st = connection.createStatement()) {
            st.execute(BossSessionDdl.DROP_STATS_SUMMARY_VIEW);
            st.execute(BossSessionDdl.CREATE_STATS_SUMMARY_VIEW);
            logger.info("boss_stats_summary view updated (ended_at IS NOT NULL filter applied).");
            return Result.success();
        } catch (Exception e) {
            return Result.failure(ErrorCode.DB_CONNECTION_FAILED,
                    "Failed to apply BossSessionMigrationV2", e);
        }
    }
}
