package kr.zenon.rpg.persistence;

import kr.zenon.rpg.common.db.ConnectionProvider;
import kr.zenon.rpg.common.logging.DomainLogger;
import kr.zenon.rpg.common.result.Result;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 성장 시계열 스냅샷 저장/조회 (성장 곡선, INBOX-004 #7 / DL-083).
 * <p>쓰기: 30분 주기 스케줄러가 온라인 플레이어 스냅샷(일별 upsert). 읽기: HTTP API /api/v1/growth/curve.</p>
 */
public final class GrowthSnapshotRepository {
    private final ConnectionProvider connectionProvider;
    private final DomainLogger logger;

    public GrowthSnapshotRepository(ConnectionProvider connectionProvider, DomainLogger logger) {
        this.connectionProvider = Objects.requireNonNull(connectionProvider, "connectionProvider");
        this.logger = Objects.requireNonNull(logger, "logger");
    }

    /** 일별 upsert — (player_uuid, snapshot_date) 충돌 시 최신값으로 갱신. */
    public void upsert(SnapshotRow row, long capturedAt) {
        String date = Instant.ofEpochMilli(capturedAt).atZone(ZoneId.systemDefault()).toLocalDate().toString();
        String sql = """
            INSERT INTO growth_snapshot
                (player_uuid, snapshot_date, player_level, avg_il, total_enhance, gold, captured_at)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT(player_uuid, snapshot_date) DO UPDATE SET
                player_level = excluded.player_level,
                avg_il = excluded.avg_il,
                total_enhance = excluded.total_enhance,
                gold = excluded.gold,
                captured_at = excluded.captured_at
            """;
        Result<Connection> conn = connectionProvider.getConnection();
        if (conn.isFailure()) return;
        try (Connection c = conn.value();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, row.uuid());
            ps.setString(2, date);
            ps.setInt(3, row.level());
            ps.setDouble(4, row.il());
            ps.setInt(5, row.totalEnhance());
            ps.setLong(6, row.gold());
            ps.setLong(7, capturedAt);
            ps.executeUpdate();
        } catch (Exception e) {
            logger.warn("GrowthSnapshotRepository.upsert failed: " + e.getMessage());
        }
    }

    /** 최근 N일 일별 평균 성장 (레벨·IL·골드·집계 플레이어 수), 최신순. */
    public List<DailyGrowth> dailyAverages(int days) {
        String sql = """
            SELECT snapshot_date,
                   COUNT(*) AS players,
                   AVG(player_level) AS avg_level,
                   AVG(avg_il) AS avg_il,
                   AVG(gold) AS avg_gold
            FROM growth_snapshot
            GROUP BY snapshot_date
            ORDER BY snapshot_date DESC
            LIMIT ?
            """;
        Result<Connection> conn = connectionProvider.getConnection();
        if (conn.isFailure()) return List.of();
        List<DailyGrowth> out = new ArrayList<>();
        try (Connection c = conn.value();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, days);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(new DailyGrowth(
                            rs.getString("snapshot_date"),
                            rs.getInt("players"),
                            Math.round(rs.getDouble("avg_level") * 10) / 10.0,
                            Math.round(rs.getDouble("avg_il") * 10) / 10.0,
                            Math.round(rs.getDouble("avg_gold"))));
                }
            }
        } catch (Exception e) {
            logger.warn("GrowthSnapshotRepository.dailyAverages failed: " + e.getMessage());
        }
        return out;
    }

    public record SnapshotRow(String uuid, int level, double il, int totalEnhance, long gold) {}

    public record DailyGrowth(String date, int players, double avgLevel, double avgIl, long avgGold) {}
}
