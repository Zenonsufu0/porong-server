package com.poro.empire.boss.db;

import com.poro.empire.boss.engine.BossResultSummary;
import com.poro.empire.common.db.ConnectionProvider;
import com.poro.empire.common.logging.DomainLogger;
import com.poro.empire.common.result.ErrorCode;
import com.poro.empire.common.result.Result;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * boss_session_log / boss_session_player 테이블 read/write.
 * <p>쓰기 흐름: recordStart → recordPlayerEntry(×N) → recordEnd</p>
 * 서버 크래시 시 result='abandoned', ended_at=NULL 레코드가 남아 실패 시도로 관측된다.
 * <p>읽기: HTTP API /api/v1/boss/* 엔드포인트용.</p>
 */
public final class BossSessionRepository {
    private final ConnectionProvider connectionProvider;
    private final DomainLogger logger;

    public BossSessionRepository(ConnectionProvider connectionProvider, DomainLogger logger) {
        this.connectionProvider = Objects.requireNonNull(connectionProvider, "connectionProvider");
        this.logger = Objects.requireNonNull(logger, "logger");
    }

    /**
     * 레이드 시작 — boss_session_log INSERT (result='abandoned' 초기값, ended_at=NULL).
     * @return 생성된 session_id, 실패 시 Result.failure
     */
    public Result<Long> recordStart(String bossId, int partySize, int seasonWeek, long startedAt) {
        String sql = """
            INSERT INTO boss_session_log
                (boss_id, season_week, started_at, party_size,
                 party_avg_enhance, party_avg_il, max_defense_ignore_pct, has_defense_ignore, result)
            VALUES (?, ?, ?, ?, NULL, NULL, 0.0, 0, 'abandoned')
            """;
        Result<Connection> connResult = connectionProvider.getConnection();
        if (connResult.isFailure()) {
            return Result.failure(connResult.errorCode(), connResult.message(), connResult.cause());
        }
        try (Connection conn = connResult.value();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, bossId);
            ps.setInt(2, seasonWeek);
            ps.setLong(3, startedAt);
            ps.setInt(4, partySize);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                long sessionId = keys.next() ? keys.getLong(1) : -1L;
                return Result.success(sessionId);
            }
        } catch (Exception e) {
            logger.warn("recordStart failed [" + bossId + "]: " + e.getMessage());
            return Result.failure(ErrorCode.DB_CONNECTION_FAILED, "recordStart failed", e);
        }
    }

    /**
     * 파티원 입장 — boss_session_player INSERT. 참여자 스펙(무기 강화·평균 강화·IL)은 실측 (DL-081).
     * defense_ignore_pct는 1차 시즌 방무 메커니즘 없음으로 0 고정.
     */
    public Result<Void> recordPlayerEntry(long sessionId, String uuid, BossParticipantSpec spec) {
        String sql = """
            INSERT INTO boss_session_player (session_id, player_uuid, weapon_enhance, avg_enhance, il, defense_ignore_pct)
            VALUES (?, ?, ?, ?, ?, 0.0)
            """;
        Result<Connection> connResult = connectionProvider.getConnection();
        if (connResult.isFailure()) {
            return Result.failure(connResult.errorCode(), connResult.message(), connResult.cause());
        }
        try (Connection conn = connResult.value();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, sessionId);
            ps.setString(2, uuid);
            ps.setInt(3, spec.weaponEnhance());
            ps.setDouble(4, spec.avgEnhance());
            ps.setDouble(5, spec.il());
            ps.executeUpdate();
            return Result.success();
        } catch (Exception e) {
            logger.warn("recordPlayerEntry failed [session=" + sessionId + ", uuid=" + uuid + "]: " + e.getMessage());
            return Result.failure(ErrorCode.DB_CONNECTION_FAILED, "recordPlayerEntry failed", e);
        }
    }

    /**
     * 해당 플레이어가 클리어(result='clear')에 참여한 보스 id 집합 — 보스 입장 게이트 영속 조회 (DL-097).
     * 재시작 후 {@code BossRoomManager}가 in-memory 클리어 기록을 복원하는 데 사용.
     */
    public Set<String> clearedBossIds(String playerUuid) {
        String sql = """
            SELECT DISTINCT s.boss_id
            FROM boss_session_log s JOIN boss_session_player p ON p.session_id = s.id
            WHERE p.player_uuid = ? AND s.result = 'clear'
            """;
        Result<Connection> connResult = connectionProvider.getConnection();
        if (connResult.isFailure()) return Set.of();
        Set<String> out = new HashSet<>();
        try (Connection conn = connResult.value();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, playerUuid);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add(rs.getString("boss_id"));
            }
        } catch (Exception e) {
            logger.warn("clearedBossIds failed [" + playerUuid + "]: " + e.getMessage());
        }
        return out;
    }

    /** 참여자 데미지 기여 점유율(%) 기록 — 종료 시 boss_session_player UPDATE (DL-084). */
    public Result<Void> recordPlayerDamage(long sessionId, String uuid, double damageShare) {
        String sql = "UPDATE boss_session_player SET damage_share = ? WHERE session_id = ? AND player_uuid = ?";
        Result<Connection> connResult = connectionProvider.getConnection();
        if (connResult.isFailure()) {
            return Result.failure(connResult.errorCode(), connResult.message(), connResult.cause());
        }
        try (Connection conn = connResult.value();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDouble(1, damageShare);
            ps.setLong(2, sessionId);
            ps.setString(3, uuid);
            ps.executeUpdate();
            return Result.success();
        } catch (Exception e) {
            logger.warn("recordPlayerDamage failed [session=" + sessionId + ", uuid=" + uuid + "]: " + e.getMessage());
            return Result.failure(ErrorCode.DB_CONNECTION_FAILED, "recordPlayerDamage failed", e);
        }
    }

    /** 파티 평균 스펙 갱신 — 전 참여자 입장 기록 후 boss_session_log UPDATE (DL-081). */
    public Result<Void> recordPartySpec(long sessionId, double avgEnhance, double avgIl) {
        String sql = "UPDATE boss_session_log SET party_avg_enhance = ?, party_avg_il = ? WHERE id = ?";
        Result<Connection> connResult = connectionProvider.getConnection();
        if (connResult.isFailure()) {
            return Result.failure(connResult.errorCode(), connResult.message(), connResult.cause());
        }
        try (Connection conn = connResult.value();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDouble(1, avgEnhance);
            ps.setDouble(2, avgIl);
            ps.setLong(3, sessionId);
            ps.executeUpdate();
            return Result.success();
        } catch (Exception e) {
            logger.warn("recordPartySpec failed [session=" + sessionId + "]: " + e.getMessage());
            return Result.failure(ErrorCode.DB_CONNECTION_FAILED, "recordPartySpec failed", e);
        }
    }

    /** 레이드 종료 — boss_session_log UPDATE (result, ended_at, clear_time_seconds). */
    public Result<Void> recordEnd(long sessionId, BossResultSummary summary) {
        long endedAt = summary.startedAt() + summary.clearTimeSeconds();
        String sql = """
            UPDATE boss_session_log
            SET result = ?, ended_at = ?, clear_time_seconds = ?
            WHERE id = ?
            """;
        Result<Connection> connResult = connectionProvider.getConnection();
        if (connResult.isFailure()) {
            return Result.failure(connResult.errorCode(), connResult.message(), connResult.cause());
        }
        try (Connection conn = connResult.value();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, toResultCode(summary));
            ps.setLong(2, endedAt);
            if (summary.clearSuccess()) {
                ps.setLong(3, summary.clearTimeSeconds());
            } else {
                ps.setNull(3, java.sql.Types.INTEGER);
            }
            ps.setLong(4, sessionId);
            ps.executeUpdate();
            return Result.success();
        } catch (Exception e) {
            logger.warn("recordEnd failed [session=" + sessionId + "]: " + e.getMessage());
            return Result.failure(ErrorCode.DB_CONNECTION_FAILED, "recordEnd failed", e);
        }
    }

    private static String toResultCode(BossResultSummary summary) {
        if (summary.clearSuccess()) return "clear";
        return switch (summary.failureReasonCode()) {
            case "timeout", "fail_berserk_timeout" -> "timeout";
            case "pattern_fail", "fail_stagger_fail" -> "pattern_fail";
            default -> "abandoned";
        };
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

    /** 플레이어별 보스 클리어 횟수 (클리어 기록 GUI용). */
    public int countClearsByPlayer(String uuid, String bossId) {
        String sql = """
            SELECT COUNT(*) FROM boss_session_log l
            JOIN boss_session_player p ON l.id = p.session_id
            WHERE p.player_uuid = ? AND l.boss_id = ? AND l.result = 'clear'
            """;
        Result<Connection> connResult = connectionProvider.getConnection();
        if (connResult.isFailure()) return 0;
        try (Connection conn = connResult.value();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, uuid);
            ps.setString(2, bossId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (Exception e) {
            logger.warn("countClearsByPlayer failed [" + uuid + ", " + bossId + "]: " + e.getMessage());
            return 0;
        }
    }

    /** 플레이어별 보스 최단 클리어 시간 (초). 미클리어 시 null. */
    public Long bestClearTimeByPlayer(String uuid, String bossId) {
        String sql = """
            SELECT MIN(l.clear_time_seconds) FROM boss_session_log l
            JOIN boss_session_player p ON l.id = p.session_id
            WHERE p.player_uuid = ? AND l.boss_id = ? AND l.result = 'clear'
            """;
        Result<Connection> connResult = connectionProvider.getConnection();
        if (connResult.isFailure()) return null;
        try (Connection conn = connResult.value();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, uuid);
            ps.setString(2, bossId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                long v = rs.getLong(1);
                return rs.wasNull() ? null : v;
            }
        } catch (Exception e) {
            logger.warn("bestClearTimeByPlayer failed [" + uuid + ", " + bossId + "]: " + e.getMessage());
            return null;
        }
    }

    /** 플레이어의 이번 주차 보스 전체 클리어 횟수 (모든 보스 합산). */
    public int countClearsThisWeekAcrossBosses(String uuid, int seasonWeek) {
        String sql = """
            SELECT COUNT(*) FROM boss_session_log l
            JOIN boss_session_player p ON l.id = p.session_id
            WHERE p.player_uuid = ? AND l.season_week = ? AND l.result = 'clear'
            """;
        Result<Connection> connResult = connectionProvider.getConnection();
        if (connResult.isFailure()) return 0;
        try (Connection conn = connResult.value();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, uuid);
            ps.setInt(2, seasonWeek);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (Exception e) {
            logger.warn("countClearsThisWeekAcrossBosses failed [" + uuid + ", week=" + seasonWeek + "]: " + e.getMessage());
            return 0;
        }
    }

    /** 서버 최단 클리어 시간 (초). 미클리어 시 null. */
    public Long serverBestClearTime(String bossId) {
        String sql = """
            SELECT MIN(clear_time_seconds) FROM boss_session_log
            WHERE boss_id = ? AND result = 'clear'
            """;
        Result<Connection> connResult = connectionProvider.getConnection();
        if (connResult.isFailure()) return null;
        try (Connection conn = connResult.value();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, bossId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                long v = rs.getLong(1);
                return rs.wasNull() ? null : v;
            }
        } catch (Exception e) {
            logger.warn("serverBestClearTime failed [" + bossId + "]: " + e.getMessage());
            return null;
        }
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
