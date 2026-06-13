package kr.zenon.rpg.common.db;

import kr.zenon.rpg.common.logging.DomainLogger;
import kr.zenon.rpg.common.result.ErrorCode;
import kr.zenon.rpg.common.result.Result;

import java.sql.Connection;
import java.sql.Statement;
import java.util.Objects;

/**
 * 런타임 운영자 설정 시스템 테이블 마이그레이션 (INBOX-010 축 A/C MVP).
 * mob_stat_override + config_change_log 생성. 멱등(IF NOT EXISTS).
 */
public final class RuntimeConfigMigration implements MigrationEntryPoint {
    private final DomainLogger logger;

    public RuntimeConfigMigration(DomainLogger logger) {
        this.logger = Objects.requireNonNull(logger, "logger");
    }

    @Override
    public Result<Void> migrate(Connection connection) {
        try (Statement statement = connection.createStatement()) {
            statement.execute(MobStatOverrideDdl.CREATE_TABLE);
            statement.execute(ConfigChangeLogDdl.CREATE_TABLE);
            statement.execute(ConfigChangeLogDdl.CREATE_INDEX_CHANGED_AT);
            logger.info("runtime config DDL applied (mob_stat_override, config_change_log).");
            return Result.success();
        } catch (Exception exception) {
            return Result.failure(
                    ErrorCode.DB_CONNECTION_FAILED,
                    "Failed to apply runtime config DDL",
                    exception);
        }
    }
}
