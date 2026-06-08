package com.poro.rpg.common.db;

import com.poro.rpg.common.logging.DomainLogger;
import com.poro.rpg.common.result.ErrorCode;
import com.poro.rpg.common.result.Result;

import java.sql.Connection;
import java.sql.Statement;
import java.util.Objects;

/** 디스코드 인증 테이블 DDL 적용 (DL-132). idempotent. */
public final class AuthMigration implements MigrationEntryPoint {
    private final DomainLogger logger;

    public AuthMigration(DomainLogger logger) {
        this.logger = Objects.requireNonNull(logger, "logger");
    }

    @Override
    public Result<Void> migrate(Connection connection) {
        try (Statement st = connection.createStatement()) {
            st.execute(AuthDdl.CREATE_AUTH_PENDING_CODE);
            st.execute(AuthDdl.CREATE_INDEX_PENDING_UUID);
            st.execute(AuthDdl.CREATE_DISCORD_LINK);
            logger.info("auth_pending_code + discord_link DDL applied (idempotent).");
            return Result.success();
        } catch (Exception e) {
            return Result.failure(ErrorCode.DB_CONNECTION_FAILED,
                    "Failed to apply auth DDL", e);
        }
    }
}
