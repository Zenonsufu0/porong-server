package com.poro.rpg.common.db;

import com.poro.rpg.common.logging.DomainLogger;
import com.poro.rpg.common.result.ErrorCode;
import com.poro.rpg.common.result.Result;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

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
            // 기존 테이블에 trace_id 누락 시 추가 (DL-089) — CREATE는 신규 설치에만 컬럼 포함
            if (!existingColumns(connection, "enhancement_log").contains("trace_id")) {
                st.execute(EnhancementLogDdl.ALTER_ADD_TRACE_ID);
                logger.info("enhancement_log.trace_id column added.");
            }
            logger.info("enhancement_log DDL applied (idempotent).");
            return Result.success();
        } catch (Exception e) {
            return Result.failure(ErrorCode.DB_CONNECTION_FAILED,
                    "Failed to apply enhancement_log DDL", e);
        }
    }

    private Set<String> existingColumns(Connection conn, String table) throws Exception {
        Set<String> cols = new HashSet<>();
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("PRAGMA table_info(" + table + ")")) {
            while (rs.next()) cols.add(rs.getString("name").toLowerCase());
        }
        return cols;
    }
}
