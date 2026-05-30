package com.poro.empire.persistence;

import com.poro.empire.common.db.ConnectionProvider;
import com.poro.empire.common.logging.DomainLogger;
import com.poro.empire.common.result.Result;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * 접속 세션 로그 저장/조회 (리텐션·DAU·플레이타임, INBOX-004 #1).
 * <p>쓰기: PlayerJoinListener (join/quit). 읽기: HTTP API /api/v1/activity/*.</p>
 * stateless DB 접근자 — 쓰기/읽기 인스턴스를 공유할 필요 없음.
 */
public final class PlayerSessionRepository {
    private final ConnectionProvider connectionProvider;
    private final DomainLogger logger;

    public PlayerSessionRepository(ConnectionProvider connectionProvider, DomainLogger logger) {
        this.connectionProvider = Objects.requireNonNull(connectionProvider, "connectionProvider");
        this.logger = Objects.requireNonNull(logger, "logger");
    }

    // ─── 쓰기 ─────────────────────────────────────────────────────────

    /** 접속 시 세션 1행 INSERT (quit_at NULL). session_date는 joined_at 기준 서버 TZ 날짜. */
    public void recordJoin(UUID uuid, String name, long joinedAt) {
        String sessionDate = Instant.ofEpochMilli(joinedAt)
                .atZone(ZoneId.systemDefault()).toLocalDate().toString();
        String sql = """
            INSERT INTO player_session_log (player_uuid, player_name, joined_at, session_date)
            VALUES (?, ?, ?, ?)
            """;
        Result<Connection> conn = connectionProvider.getConnection();
        if (conn.isFailure()) return;
        try (Connection c = conn.value();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            ps.setString(2, name);
            ps.setLong(3, joinedAt);
            ps.setString(4, sessionDate);
            ps.executeUpdate();
        } catch (Exception e) {
            logger.warn("PlayerSessionRepository.recordJoin failed: " + e.getMessage());
        }
    }

    /** 종료 시 가장 최근 열린(quit_at IS NULL) 세션을 닫고 duration_s 계산. */
    public void recordQuit(UUID uuid, long quitAt) {
        String sql = """
            UPDATE player_session_log
            SET quit_at = ?, duration_s = (? - joined_at) / 1000
            WHERE id = (
                SELECT id FROM player_session_log
                WHERE player_uuid = ? AND quit_at IS NULL
                ORDER BY joined_at DESC LIMIT 1
            )
            """;
        Result<Connection> conn = connectionProvider.getConnection();
        if (conn.isFailure()) return;
        try (Connection c = conn.value();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, quitAt);
            ps.setLong(2, quitAt);
            ps.setString(3, uuid.toString());
            ps.executeUpdate();
        } catch (Exception e) {
            logger.warn("PlayerSessionRepository.recordQuit failed: " + e.getMessage());
        }
    }

    /** 정상 종료(onDisable) 시 남은 열린 세션 일괄 마감 — 크래시 dangling 방지. */
    public int closeOpenSessions(long now) {
        String sql = """
            UPDATE player_session_log
            SET quit_at = ?, duration_s = (? - joined_at) / 1000
            WHERE quit_at IS NULL
            """;
        Result<Connection> conn = connectionProvider.getConnection();
        if (conn.isFailure()) return 0;
        try (Connection c = conn.value();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, now);
            ps.setLong(2, now);
            return ps.executeUpdate();
        } catch (Exception e) {
            logger.warn("PlayerSessionRepository.closeOpenSessions failed: " + e.getMessage());
            return 0;
        }
    }

    // ─── 읽기 (API) ───────────────────────────────────────────────────

    /** 최근 N일 DAU (날짜별 고유 접속자·세션 수), 최신순. */
    public List<DauRow> dauLast(int days) {
        String sql = """
            SELECT session_date, COUNT(DISTINCT player_uuid) AS uniq, COUNT(*) AS sessions
            FROM player_session_log
            GROUP BY session_date
            ORDER BY session_date DESC
            LIMIT ?
            """;
        Result<Connection> conn = connectionProvider.getConnection();
        if (conn.isFailure()) return List.of();
        List<DauRow> out = new ArrayList<>();
        try (Connection c = conn.value();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, days);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(new DauRow(rs.getString(1), rs.getInt(2), rs.getInt(3)));
                }
            }
        } catch (Exception e) {
            logger.warn("PlayerSessionRepository.dauLast failed: " + e.getMessage());
        }
        return out;
    }

    /** 전체 활동 요약 — 총 세션·고유 플레이어·평균 세션 길이(완료 세션 한정). */
    public ActivitySummary summary() {
        String sql = """
            SELECT COUNT(*) AS total,
                   COUNT(DISTINCT player_uuid) AS uniq,
                   AVG(CASE WHEN duration_s IS NOT NULL THEN duration_s END) AS avg_dur,
                   SUM(CASE WHEN duration_s IS NOT NULL THEN 1 ELSE 0 END) AS completed
            FROM player_session_log
            """;
        Result<Connection> conn = connectionProvider.getConnection();
        if (conn.isFailure()) return new ActivitySummary(0, 0, 0, 0);
        try (Connection c = conn.value();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return new ActivitySummary(
                        rs.getInt("total"),
                        rs.getInt("uniq"),
                        Math.round(rs.getDouble("avg_dur")),
                        rs.getInt("completed"));
            }
        } catch (Exception e) {
            logger.warn("PlayerSessionRepository.summary failed: " + e.getMessage());
        }
        return new ActivitySummary(0, 0, 0, 0);
    }

    public record DauRow(String date, int uniquePlayers, int sessions) {}

    public record ActivitySummary(int totalSessions, int totalUniquePlayers,
                                  long avgSessionSeconds, int completedSessions) {}
}
