package com.poro.rpg.persistence;

import com.poro.rpg.common.db.ConnectionProvider;
import com.poro.rpg.common.logging.DomainLogger;
import com.poro.rpg.common.result.Result;
import com.poro.rpg.growth.engine.CurrencyFlowListener;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 통화 흐름 저장/조회 (골드 인플레이션/싱크, INBOX-004 #2 / DL-080).
 * <p>{@link CurrencyFlowListener}로 PlayerGrowthState에 부착되어 실제 변동 시 INSERT.
 * 읽기는 HTTP API /api/v1/economy/flow.</p>
 */
public final class EconomyFlowRepository implements CurrencyFlowListener {
    private final ConnectionProvider connectionProvider;
    private final DomainLogger logger;

    public EconomyFlowRepository(ConnectionProvider connectionProvider, DomainLogger logger) {
        this.connectionProvider = Objects.requireNonNull(connectionProvider, "connectionProvider");
        this.logger = Objects.requireNonNull(logger, "logger");
    }

    // ─── 쓰기 (CurrencyFlowListener) ──────────────────────────────────

    @Override
    public void onFlow(String userId, boolean inflow, String currency, long amount) {
        long now = System.currentTimeMillis();
        String flowDate = Instant.ofEpochMilli(now).atZone(ZoneId.systemDefault()).toLocalDate().toString();
        String sql = """
            INSERT INTO economy_flow (player_uuid, direction, currency, amount, occurred_at, flow_date)
            VALUES (?, ?, ?, ?, ?, ?)
            """;
        Result<Connection> conn = connectionProvider.getConnection();
        if (conn.isFailure()) return;
        try (Connection c = conn.value();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, userId);
            ps.setString(2, inflow ? "inflow" : "outflow");
            ps.setString(3, currency);
            ps.setLong(4, amount);
            ps.setLong(5, now);
            ps.setString(6, flowDate);
            ps.executeUpdate();
        } catch (Exception e) {
            logger.warn("EconomyFlowRepository.onFlow failed: " + e.getMessage());
        }
    }

    // ─── 읽기 (API) ───────────────────────────────────────────────────

    /** 통화별 발행량(inflow)·소각량(outflow)·net 요약. */
    public List<CurrencyNet> netByCurrency() {
        String sql = """
            SELECT currency,
                   SUM(CASE WHEN direction='inflow'  THEN amount ELSE 0 END) AS inflow,
                   SUM(CASE WHEN direction='outflow' THEN amount ELSE 0 END) AS outflow
            FROM economy_flow
            GROUP BY currency
            ORDER BY currency
            """;
        Result<Connection> conn = connectionProvider.getConnection();
        if (conn.isFailure()) return List.of();
        List<CurrencyNet> out = new ArrayList<>();
        try (Connection c = conn.value();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                long in = rs.getLong("inflow");
                long outv = rs.getLong("outflow");
                out.add(new CurrencyNet(rs.getString("currency"), in, outv, in - outv));
            }
        } catch (Exception e) {
            logger.warn("EconomyFlowRepository.netByCurrency failed: " + e.getMessage());
        }
        return out;
    }

    /** 특정 통화의 최근 N일 일별 발행/소각/net, 최신순. */
    public List<DailyNet> dailyNet(String currency, int days) {
        String sql = """
            SELECT flow_date,
                   SUM(CASE WHEN direction='inflow'  THEN amount ELSE 0 END) AS inflow,
                   SUM(CASE WHEN direction='outflow' THEN amount ELSE 0 END) AS outflow
            FROM economy_flow
            WHERE currency = ?
            GROUP BY flow_date
            ORDER BY flow_date DESC
            LIMIT ?
            """;
        Result<Connection> conn = connectionProvider.getConnection();
        if (conn.isFailure()) return List.of();
        List<DailyNet> out = new ArrayList<>();
        try (Connection c = conn.value();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, currency);
            ps.setInt(2, days);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    long in = rs.getLong("inflow");
                    long outv = rs.getLong("outflow");
                    out.add(new DailyNet(rs.getString("flow_date"), in, outv, in - outv));
                }
            }
        } catch (Exception e) {
            logger.warn("EconomyFlowRepository.dailyNet failed: " + e.getMessage());
        }
        return out;
    }

    public record CurrencyNet(String currency, long inflow, long outflow, long net) {}

    public record DailyNet(String date, long inflow, long outflow, long net) {}
}
