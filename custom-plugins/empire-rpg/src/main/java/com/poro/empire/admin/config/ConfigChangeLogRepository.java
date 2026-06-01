package com.poro.empire.admin.config;

import com.poro.empire.common.db.ConnectionProvider;
import com.poro.empire.common.logging.DomainLogger;
import com.poro.empire.common.result.Result;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** config_change_log — 런타임 설정 변경 감사 로그(축 C). 향후 패치노트 피드 원천. */
public final class ConfigChangeLogRepository {

    public record Entry(long changedAt, String changedBy, String domain,
                        String targetKey, String field, String oldValue, String newValue) {}

    private final ConnectionProvider connectionProvider;
    private final DomainLogger logger;

    public ConfigChangeLogRepository(ConnectionProvider connectionProvider, DomainLogger logger) {
        this.connectionProvider = Objects.requireNonNull(connectionProvider);
        this.logger             = Objects.requireNonNull(logger);
    }

    public void append(String domain, String targetKey, String field,
                       String oldValue, String newValue, String changedBy) {
        String sql = """
            INSERT INTO config_change_log (changed_at, changed_by, domain, target_key, field, old_value, new_value)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """;
        try (Connection c = openConn();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, System.currentTimeMillis());
            ps.setString(2, changedBy);
            ps.setString(3, domain);
            ps.setString(4, targetKey);
            ps.setString(5, field);
            ps.setString(6, oldValue);
            ps.setString(7, newValue);
            ps.executeUpdate();
        } catch (Exception e) {
            logger.warn("config_change_log append 실패 [" + domain + "/" + targetKey + "]: " + e.getMessage());
        }
    }

    /** 최근 변경 N건 (패치노트/로그 조회용). */
    public List<Entry> recent(int limit) {
        List<Entry> out = new ArrayList<>();
        String sql = """
            SELECT changed_at, changed_by, domain, target_key, field, old_value, new_value
            FROM config_change_log ORDER BY id DESC LIMIT ?
            """;
        try (Connection c = openConn();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, Math.max(1, limit));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(new Entry(
                            rs.getLong("changed_at"),
                            rs.getString("changed_by"),
                            rs.getString("domain"),
                            rs.getString("target_key"),
                            rs.getString("field"),
                            rs.getString("old_value"),
                            rs.getString("new_value")));
                }
            }
        } catch (Exception e) {
            logger.warn("config_change_log recent 실패: " + e.getMessage());
        }
        return out;
    }

    private Connection openConn() throws Exception {
        Result<Connection> r = connectionProvider.getConnection();
        if (r.isFailure()) throw new Exception("DB connection failed: " + r.message());
        return r.value();
    }
}
