package com.poro.empire.common.db;

import com.poro.empire.common.logging.DomainLogger;
import com.poro.empire.common.result.ErrorCode;
import com.poro.empire.common.result.Result;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * boss_session_player에 damage_share 컬럼 추가 (DL-084, INBOX-004 #5).
 * PRAGMA table_info로 기존 컬럼 확인 후 누락 시에만 ALTER — idempotent.
 */
public final class BossSessionPlayerMigrationV3 implements MigrationEntryPoint {
    private final DomainLogger logger;

    public BossSessionPlayerMigrationV3(DomainLogger logger) {
        this.logger = Objects.requireNonNull(logger, "logger");
    }

    @Override
    public Result<Void> migrate(Connection connection) {
        try {
            Set<String> cols = new HashSet<>();
            try (Statement st = connection.createStatement();
                 ResultSet rs = st.executeQuery("PRAGMA table_info(boss_session_player)")) {
                while (rs.next()) cols.add(rs.getString("name").toLowerCase());
            }
            // 테이블이 없으면 BossSessionMigration의 CREATE가 새 컬럼 포함본으로 생성하므로 skip
            if (cols.isEmpty() || cols.contains("damage_share")) {
                return Result.success();
            }
            try (Statement st = connection.createStatement()) {
                st.execute(BossSessionDdl.ALTER_SESSION_PLAYER_DAMAGE_SHARE);
            }
            logger.info("boss_session_player.damage_share column added.");
            return Result.success();
        } catch (Exception e) {
            return Result.failure(ErrorCode.DB_CONNECTION_FAILED,
                    "Failed to apply BossSessionPlayerMigrationV3", e);
        }
    }
}
