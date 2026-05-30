package com.poro.empire.growth.engine;

import com.poro.empire.common.db.ConnectionProvider;
import com.poro.empire.common.logging.DomainLogger;
import com.poro.empire.common.result.Result;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 강화 시도 로그 DB 영속 hook (INBOX-004 #3 / DL-079).
 * <p>쓰기: EnhancementService → onAttempt(INSERT). 읽기: HTTP API /api/v1/economy/enhancement.</p>
 * in-memory hook과 합성(CompositeEnhancementLogHook)되어 함께 기록된다 — in-memory는 관리자 GUI 최근 조회,
 * DB는 누적 분석(표기 vs 실제 성공률, 유저당 강화 부담)용.
 */
public final class DbEnhancementLogHook implements EnhancementLogHook {
    private final ConnectionProvider connectionProvider;
    private final DomainLogger logger;

    public DbEnhancementLogHook(ConnectionProvider connectionProvider, DomainLogger logger) {
        this.connectionProvider = Objects.requireNonNull(connectionProvider, "connectionProvider");
        this.logger = Objects.requireNonNull(logger, "logger");
    }

    @Override
    public void onAttempt(EnhancementService.EnhancementResult r) {
        String sql = """
            INSERT INTO enhancement_log
                (player_uuid, item_id, tier, before_level, target_level, final_level,
                 success, success_rate, roll, gold_cost, stone_cost, forced_ceiling, attempted_at, trace_id)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;
        Result<Connection> conn = connectionProvider.getConnection();
        if (conn.isFailure()) return;
        try (Connection c = conn.value();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, r.userId());
            ps.setString(2, r.itemId());
            ps.setString(3, r.tier());
            ps.setInt(4, r.beforeLevel());
            ps.setInt(5, r.targetLevel());
            ps.setInt(6, r.finalLevel());
            ps.setInt(7, r.success() ? 1 : 0);
            ps.setDouble(8, r.successRate());
            ps.setDouble(9, r.roll());
            ps.setLong(10, r.goldCost());
            ps.setLong(11, r.stoneCost());
            ps.setInt(12, r.forcedByCeiling() ? 1 : 0);
            ps.setLong(13, System.currentTimeMillis());
            ps.setString(14, r.traceId()); // 미사용 시 NULL
            ps.executeUpdate();
        } catch (Exception e) {
            logger.warn("DbEnhancementLogHook.onAttempt failed: " + e.getMessage());
        }
    }

    // ─── 읽기 (API) ───────────────────────────────────────────────────

    /** 티어·목표강화 단계별 성공률 — 표기(nominal) vs 실제(actual) 대조. */
    public List<TierLevelStat> statsByTierLevel() {
        String sql = """
            SELECT tier, target_level,
                   COUNT(*) AS attempts,
                   SUM(success) AS successes,
                   AVG(success_rate) AS nominal_rate,
                   SUM(forced_ceiling) AS forced
            FROM enhancement_log
            GROUP BY tier, target_level
            ORDER BY tier, target_level
            """;
        Result<Connection> conn = connectionProvider.getConnection();
        if (conn.isFailure()) return List.of();
        List<TierLevelStat> out = new ArrayList<>();
        try (Connection c = conn.value();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                int attempts = rs.getInt("attempts");
                int successes = rs.getInt("successes");
                double actual = attempts > 0 ? 100.0 * successes / attempts : 0.0;
                out.add(new TierLevelStat(
                        rs.getString("tier"), rs.getInt("target_level"),
                        attempts, successes,
                        Math.round(actual * 10) / 10.0,
                        Math.round(rs.getDouble("nominal_rate") * 10) / 10.0,
                        rs.getInt("forced")));
            }
        } catch (Exception e) {
            logger.warn("DbEnhancementLogHook.statsByTierLevel failed: " + e.getMessage());
        }
        return out;
    }

    /** 전체 강화 요약 — 총 시도·성공·실제 성공률·총 소모(골드·강화석)·천장 발동 수. */
    public EnhancementSummary summary() {
        String sql = """
            SELECT COUNT(*) AS attempts,
                   SUM(success) AS successes,
                   SUM(gold_cost) AS gold,
                   SUM(stone_cost) AS stone,
                   SUM(forced_ceiling) AS forced
            FROM enhancement_log
            """;
        Result<Connection> conn = connectionProvider.getConnection();
        if (conn.isFailure()) return new EnhancementSummary(0, 0, 0.0, 0, 0, 0);
        try (Connection c = conn.value();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                int attempts = rs.getInt("attempts");
                int successes = rs.getInt("successes");
                double actual = attempts > 0 ? 100.0 * successes / attempts : 0.0;
                return new EnhancementSummary(attempts, successes,
                        Math.round(actual * 10) / 10.0,
                        rs.getLong("gold"), rs.getLong("stone"), rs.getInt("forced"));
            }
        } catch (Exception e) {
            logger.warn("DbEnhancementLogHook.summary failed: " + e.getMessage());
        }
        return new EnhancementSummary(0, 0, 0.0, 0, 0, 0);
    }

    public record TierLevelStat(String tier, int targetLevel, int attempts, int successes,
                                double actualRatePct, double nominalRatePct, int forcedCeiling) {}

    public record EnhancementSummary(int totalAttempts, int totalSuccesses, double actualRatePct,
                                     long totalGold, long totalStone, int forcedCeiling) {}
}
