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

/**
 * pvp_match_log에 무기/IL 컬럼 추가 (DL-082, INBOX-004 #6).
 * PRAGMA table_info로 기존 컬럼을 확인해 누락분만 ALTER — idempotent.
 */
public final class PvpMatchLogMigrationV2 implements MigrationEntryPoint {
    private final DomainLogger logger;

    public PvpMatchLogMigrationV2(DomainLogger logger) {
        this.logger = Objects.requireNonNull(logger, "logger");
    }

    @Override
    public Result<Void> migrate(Connection connection) {
        try {
            Set<String> existing = existingColumns(connection, "pvp_match_log");
            // 테이블이 아직 없으면 PvpMigration의 CREATE가 새 컬럼 포함본으로 생성하므로 skip
            if (existing.isEmpty()) {
                return Result.success();
            }
            int added = 0;
            for (String alter : PvpDdl.ALTER_MATCH_LOG_WEAPON_IL) {
                String col = columnNameOf(alter);
                if (col != null && existing.contains(col)) continue;
                try (Statement st = connection.createStatement()) {
                    st.execute(alter);
                    added++;
                }
            }
            if (added > 0) logger.info("pvp_match_log weapon/IL columns added: " + added);
            return Result.success();
        } catch (Exception e) {
            return Result.failure(ErrorCode.DB_CONNECTION_FAILED,
                    "Failed to apply PvpMatchLogMigrationV2", e);
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

    /** "ALTER TABLE x ADD COLUMN <name> TYPE" → <name> (소문자). */
    private String columnNameOf(String alter) {
        String marker = "ADD COLUMN ";
        int i = alter.indexOf(marker);
        if (i < 0) return null;
        String rest = alter.substring(i + marker.length()).trim();
        int sp = rest.indexOf(' ');
        return (sp < 0 ? rest : rest.substring(0, sp)).toLowerCase();
    }
}
