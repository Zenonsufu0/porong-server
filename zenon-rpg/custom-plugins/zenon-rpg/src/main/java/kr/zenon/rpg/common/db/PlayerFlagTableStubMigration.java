package kr.zenon.rpg.common.db;

import kr.zenon.rpg.common.logging.DomainLogger;
import kr.zenon.rpg.common.result.ErrorCode;
import kr.zenon.rpg.common.result.Result;

import java.sql.Connection;
import java.sql.Statement;
import java.util.Objects;

/**
 * v0.1 한시적 스텁. PR1.5에서 마이그레이션 러너로 이관 후 삭제 예정.
 */
public final class PlayerFlagTableStubMigration implements MigrationEntryPoint {
    private final DomainLogger logger;

    public PlayerFlagTableStubMigration(DomainLogger logger) {
        this.logger = Objects.requireNonNull(logger, "logger");
    }

    @Override
    public Result<Void> migrate(Connection connection) {
        try (Statement statement = connection.createStatement()) {
            statement.execute(PlayerFlagDdl.CREATE_TABLE);
            statement.execute(PlayerFlagDdl.CREATE_INDEX_UPDATED_AT);
            statement.execute(PlayerFlagDdl.CREATE_INDEX_FLAG_KEY);
            logger.info("player_flag stub DDL applied (idempotent).");
            return Result.success();
        } catch (Exception exception) {
            return Result.failure(
                    ErrorCode.DB_CONNECTION_FAILED,
                    "Failed to apply player_flag stub DDL",
                    exception);
        }
    }
}
