package com.poro.empire.pvp.db;

import com.poro.empire.common.db.ConnectionProvider;
import com.poro.empire.common.logging.DomainLogger;
import com.poro.empire.common.result.Result;
import com.poro.empire.pvp.PvpMatchType;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Types;
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
}
