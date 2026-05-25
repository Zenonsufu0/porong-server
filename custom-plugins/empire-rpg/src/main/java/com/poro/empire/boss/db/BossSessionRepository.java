package com.poro.empire.boss.db;

import com.poro.empire.boss.engine.BossResultSummary;
import com.poro.empire.common.db.ConnectionProvider;
import com.poro.empire.common.logging.DomainLogger;
import com.poro.empire.common.result.Result;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * boss_session_log / boss_session_player 테이블 read/write.
 * 쓰기: BossEngineRuntime 세션 종료 시 호출 (§7+ 연결 예정).
 * 읽기: HTTP API /api/v1/boss/* 엔드포인트용.
 */
public final class BossSessionRepository {
    private final ConnectionProvider connectionProvider;
    private final DomainLogger logger;

    public BossSessionRepository(ConnectionProvider connectionProvider, DomainLogger logger) {
        this.connectionProvider = Objects.requireNonNull(connectionProvider, "connectionProvider");
        this.logger = Objects.requireNonNull(logger, "logger");
    }

    /** 보스 세션 결과를 DB에 기록한다. 참여자 스펙은 placeholder 0.0으로 저장된다 (§7+ 연결 후 실수치 교체 예정). */
    public Result<Void> record(BossResultSummary summary, int partySize, int seasonWeek, long startedAt, long endedAt) {
        Result<Connection> connResult = connectionProvider.getConnection();
        if (connResult.isFailure()) {
            return Result.failure(connResult.errorCode(), connResult.message(), connResult.cause());
        }
        try (Connection conn = connResult.value()) {
            conn.setAutoCommit(false);
            long sessionId = insertSession(conn, summary, partySize, seasonWeek, startedAt, endedAt);
            for (Map<String, Object> participant : summary.participantSummaryPlaceholder()) {
                String uuid = String.valueOf(participant.getOrDefault("user_id", ""));
                insertSessionPlayer(conn, sessionId, uuid, 0, 0.0f, 0.0f, 0.0f);
            }
            conn.commit();
            return Result.success();
        } catch (Exception e) {
            logger.warn("Failed to record boss session: " + e.getMessage());
            return Result.failure(com.poro.empire.common.result.ErrorCode.DB_CONNECTION_FAILED,
                    "Failed to record boss session", e);
        }
    }

    private long insertSession(Connection conn, BossResultSummary summary,
                               int partySize, int seasonWeek, long startedAt, long endedAt) throws Exception {
        String sql = """
            INSERT INTO boss_session_log
                (boss_id, season_week, started_at, ended_at, result, clear_time_seconds,
                 party_size, party_avg_enhance, party_avg_il, max_defense_ignore_pct, has_defense_ignore)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, summary.bossId());
            ps.setInt(2, seasonWeek);
            ps.setLong(3, startedAt);
            ps.setLong(4, endedAt);
            ps.setString(5, toResultCode(summary));
            if (summary.clearSuccess()) {
                ps.setLong(6, summary.clearTimeSeconds());
            } else {
                ps.setNull(6, java.sql.Types.INTEGER);
            }
            ps.setInt(7, partySize);
            ps.setNull(8, java.sql.Types.REAL);
            ps.setNull(9, java.sql.Types.REAL);
            ps.setDouble(10, 0.0);
            ps.setInt(11, 0);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                return keys.next() ? keys.getLong(1) : -1L;
            }
        }
    }

    private void insertSessionPlayer(Connection conn, long sessionId, String uuid,
                                     int weaponEnhance, float avgEnhance, float il, float defenseIgnorePct)
            throws Exception {
        String sql = """
            INSERT INTO boss_session_player (session_id, player_uuid, weapon_enhance, avg_enhance, il, defense_ignore_pct)
            VALUES (?, ?, ?, ?, ?, ?)
            """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, sessionId);
            ps.setString(2, uuid);
            ps.setInt(3, weaponEnhance);
            ps.setFloat(4, avgEnhance);
            ps.setFloat(5, il);
            ps.setFloat(6, defenseIgnorePct);
            ps.executeUpdate();
        }
    }

    private static String toResultCode(BossResultSummary summary) {
        if (summary.clearSuccess()) return "clear";
        String code = summary.failureReasonCode();
        if ("timeout".equals(code) || "pattern_fail".equals(code)) return code;
        return "abandoned";
    }

    /** GET /boss/stats — boss_stats_summary 뷰 전체 조회. */
    public List<Map<String, Object>> queryAllStats() {
        String sql = "SELECT * FROM boss_stats_summary ORDER BY boss_id";
        return queryRows(sql);
    }

    /** GET /boss/{boss_id}/stats — 단일 보스 상세 통계. */
    public Map<String, Object> queryStatsByBossId(String bossId) {
        String sql = "SELECT * FROM boss_stats_summary WHERE boss_id = ?";
        List<Map<String, Object>> rows = queryRowsParam(sql, bossId);
        return rows.isEmpty() ? Map.of("boss_id", bossId, "total_attempts", 0) : rows.get(0);
    }

    /** GET /boss/{boss_id}/weekly — 주차별 클리어율 추이. */
    public List<Map<String, Object>> queryWeekly(String bossId) {
        String sql = """
            SELECT season_week,
                   COUNT(*) AS attempts,
                   SUM(CASE WHEN result = 'clear' THEN 1 ELSE 0 END) AS clears,
                   ROUND(100.0 * SUM(CASE WHEN result = 'clear' THEN 1 ELSE 0 END) / COUNT(*), 1) AS clear_rate_pct
            FROM boss_session_log
            WHERE boss_id = ?
            GROUP BY season_week
            ORDER BY season_week
            """;
        return queryRowsParam(sql, bossId);
    }

    /** GET /boss/{boss_id}/party-spec — 클리어 파티 스펙 분포 (강화·방무 포함). */
    public List<Map<String, Object>> queryPartySpec(String bossId) {
        String sql = """
            SELECT party_avg_enhance, party_avg_il, party_size, clear_time_seconds,
                   max_defense_ignore_pct, has_defense_ignore
            FROM boss_session_log
            WHERE boss_id = ? AND result = 'clear'
            ORDER BY id DESC
            LIMIT 200
            """;
        return queryRowsParam(sql, bossId);
    }

    private List<Map<String, Object>> queryRows(String sql) {
        Result<Connection> connResult = connectionProvider.getConnection();
        if (connResult.isFailure()) return List.of();
        try (Connection conn = connResult.value();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            return toList(rs);
        } catch (Exception e) {
            logger.warn("queryRows failed: " + e.getMessage());
            return List.of();
        }
    }

    private List<Map<String, Object>> queryRowsParam(String sql, String param) {
        Result<Connection> connResult = connectionProvider.getConnection();
        if (connResult.isFailure()) return List.of();
        try (Connection conn = connResult.value();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, param);
            try (ResultSet rs = ps.executeQuery()) {
                return toList(rs);
            }
        } catch (Exception e) {
            logger.warn("queryRowsParam failed [" + param + "]: " + e.getMessage());
            return List.of();
        }
    }

    private static List<Map<String, Object>> toList(ResultSet rs) throws Exception {
        List<Map<String, Object>> rows = new ArrayList<>();
        int colCount = rs.getMetaData().getColumnCount();
        while (rs.next()) {
            Map<String, Object> row = new LinkedHashMap<>();
            for (int i = 1; i <= colCount; i++) {
                row.put(rs.getMetaData().getColumnLabel(i), rs.getObject(i));
            }
            rows.add(row);
        }
        return rows;
    }
}
