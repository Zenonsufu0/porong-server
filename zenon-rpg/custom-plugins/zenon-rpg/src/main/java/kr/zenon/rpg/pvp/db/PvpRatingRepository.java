package kr.zenon.rpg.pvp.db;

import kr.zenon.rpg.common.db.ConnectionProvider;
import kr.zenon.rpg.common.logging.DomainLogger;
import kr.zenon.rpg.common.result.Result;
import kr.zenon.rpg.pvp.PvpRatingService;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class PvpRatingRepository {
    private final ConnectionProvider connectionProvider;
    private final DomainLogger logger;

    public PvpRatingRepository(ConnectionProvider connectionProvider, DomainLogger logger) {
        this.connectionProvider = Objects.requireNonNull(connectionProvider);
        this.logger             = Objects.requireNonNull(logger);
    }

    /** UUID로 점수 조회. 없으면 empty. */
    public Optional<PvpRatingService.Rating> find(UUID uuid) {
        String sql = "SELECT player_uuid, player_name, score, wins, losses FROM pvp_rating WHERE player_uuid = ?";
        Result<Connection> conn = connectionProvider.getConnection();
        if (conn.isFailure()) return Optional.empty();
        try (Connection c = conn.value();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(new PvpRatingService.Rating(
                        UUID.fromString(rs.getString("player_uuid")),
                        rs.getString("player_name"),
                        rs.getInt("score"),
                        rs.getInt("wins"),
                        rs.getInt("losses")
                ));
            }
        } catch (Exception e) {
            logger.warn("PvpRatingRepository.find failed [" + uuid + "]: " + e.getMessage());
        }
        return Optional.empty();
    }

    /** INSERT or UPDATE (upsert). */
    public void save(PvpRatingService.Rating r) {
        String sql = """
            INSERT INTO pvp_rating (player_uuid, player_name, score, wins, losses, updated_at)
            VALUES (?, ?, ?, ?, ?, ?)
            ON CONFLICT(player_uuid) DO UPDATE SET
                player_name = excluded.player_name,
                score       = excluded.score,
                wins        = excluded.wins,
                losses      = excluded.losses,
                updated_at  = excluded.updated_at
            """;
        Result<Connection> conn = connectionProvider.getConnection();
        if (conn.isFailure()) return;
        try (Connection c = conn.value();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, r.uuid().toString());
            ps.setString(2, r.name());
            ps.setInt(3, r.score());
            ps.setInt(4, r.wins());
            ps.setInt(5, r.losses());
            ps.setLong(6, System.currentTimeMillis());
            ps.executeUpdate();
        } catch (Exception e) {
            logger.warn("PvpRatingRepository.save failed [" + r.uuid() + "]: " + e.getMessage());
        }
    }

    /** 전체 점수 desc 정렬 조회 (limit). */
    public List<PvpRatingService.Rating> top(int limit) {
        String sql = "SELECT player_uuid, player_name, score, wins, losses FROM pvp_rating ORDER BY score DESC, wins DESC LIMIT ?";
        Result<Connection> conn = connectionProvider.getConnection();
        if (conn.isFailure()) return List.of();
        List<PvpRatingService.Rating> out = new ArrayList<>();
        try (Connection c = conn.value();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(new PvpRatingService.Rating(
                            UUID.fromString(rs.getString("player_uuid")),
                            rs.getString("player_name"),
                            rs.getInt("score"),
                            rs.getInt("wins"),
                            rs.getInt("losses")
                    ));
                }
            }
        } catch (Exception e) {
            logger.warn("PvpRatingRepository.top failed: " + e.getMessage());
        }
        return out;
    }

    /** 모든 행 로드 (서버 시작 시 in-memory 캐시 채움용). */
    public List<PvpRatingService.Rating> loadAll() {
        String sql = "SELECT player_uuid, player_name, score, wins, losses FROM pvp_rating";
        Result<Connection> conn = connectionProvider.getConnection();
        if (conn.isFailure()) return List.of();
        List<PvpRatingService.Rating> out = new ArrayList<>();
        try (Connection c = conn.value();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                out.add(new PvpRatingService.Rating(
                        UUID.fromString(rs.getString("player_uuid")),
                        rs.getString("player_name"),
                        rs.getInt("score"),
                        rs.getInt("wins"),
                        rs.getInt("losses")
                ));
            }
        } catch (Exception e) {
            logger.warn("PvpRatingRepository.loadAll failed: " + e.getMessage());
        }
        return out;
    }
}
