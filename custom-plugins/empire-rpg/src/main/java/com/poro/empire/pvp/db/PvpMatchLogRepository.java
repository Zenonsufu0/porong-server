package com.poro.empire.pvp.db;

import com.poro.empire.common.db.ConnectionProvider;
import com.poro.empire.common.logging.DomainLogger;
import com.poro.empire.common.result.Result;
import com.poro.empire.pvp.PvpMatchType;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class PvpMatchLogRepository {
    private final ConnectionProvider connectionProvider;
    private final DomainLogger logger;

    public PvpMatchLogRepository(ConnectionProvider connectionProvider, DomainLogger logger) {
        this.connectionProvider = Objects.requireNonNull(connectionProvider);
        this.logger             = Objects.requireNonNull(logger);
    }

    public void record(PvpMatchType type, UUID winner, UUID loser, boolean draw, int durationS, String reason) {
        String sql = """
            INSERT INTO pvp_match_log (match_type, winner_uuid, loser_uuid, draw, duration_s, reason, played_at)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """;
        Result<Connection> conn = connectionProvider.getConnection();
        if (conn.isFailure()) return;
        try (Connection c = conn.value();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, type.name());
            if (winner != null) ps.setString(2, winner.toString()); else ps.setNull(2, Types.VARCHAR);
            if (loser  != null) ps.setString(3, loser.toString());  else ps.setNull(3, Types.VARCHAR);
            ps.setInt(4, draw ? 1 : 0);
            ps.setInt(5, durationS);
            ps.setString(6, reason);
            ps.setLong(7, System.currentTimeMillis());
            ps.executeUpdate();
        } catch (Exception e) {
            logger.warn("PvpMatchLogRepository.record failed: " + e.getMessage());
        }
    }

    /** 관리자 로그 GUI/명령어용 — 최근 매치 N건 (played_at DESC). UUID는 표시 시점에 이름 해석. */
    public List<PvpMatchLogRow> recentMatches(int limit) {
        String sql = """
            SELECT match_type, winner_uuid, loser_uuid, draw, duration_s, reason, played_at
            FROM pvp_match_log ORDER BY played_at DESC LIMIT ?
            """;
        Result<Connection> conn = connectionProvider.getConnection();
        if (conn.isFailure()) return List.of();
        List<PvpMatchLogRow> out = new ArrayList<>();
        try (Connection c = conn.value();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(new PvpMatchLogRow(
                            rs.getString(1), rs.getString(2), rs.getString(3),
                            rs.getInt(4) != 0, rs.getInt(5), rs.getString(6), rs.getLong(7)));
                }
            }
        } catch (Exception e) {
            logger.warn("PvpMatchLogRepository.recentMatches failed: " + e.getMessage());
        }
        return out;
    }

    /** 매치 로그 1행. winner/loser UUID는 무승부 시 null 가능. */
    public record PvpMatchLogRow(
            String matchType, String winnerUuid, String loserUuid,
            boolean draw, int durationS, String reason, long playedAt) {}
}
