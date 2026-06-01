package com.poro.rpg.operations.query.service;

import com.poro.rpg.operations.query.model.DashboardAlert;
import com.poro.rpg.operations.query.model.EconomyFlowRecord;
import com.poro.rpg.operations.query.model.QueryTimeRange;
import com.poro.rpg.operations.query.store.OperationsDataStore;

import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public final class AdminDashboardService {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneOffset.UTC);

    private final BossStatisticsQueryService bossStatisticsQueryService;
    private final MarketStatisticsQueryService marketStatisticsQueryService;
    private final LifeStatisticsQueryService lifeStatisticsQueryService;
    private final OperationsDataStore dataStore;

    public AdminDashboardService(
            BossStatisticsQueryService bossStatisticsQueryService,
            MarketStatisticsQueryService marketStatisticsQueryService,
            LifeStatisticsQueryService lifeStatisticsQueryService,
            OperationsDataStore dataStore
    ) {
        this.bossStatisticsQueryService = Objects.requireNonNull(bossStatisticsQueryService, "bossStatisticsQueryService");
        this.marketStatisticsQueryService = Objects.requireNonNull(marketStatisticsQueryService, "marketStatisticsQueryService");
        this.lifeStatisticsQueryService = Objects.requireNonNull(lifeStatisticsQueryService, "lifeStatisticsQueryService");
        this.dataStore = Objects.requireNonNull(dataStore, "dataStore");
    }

    public OverviewSummary overviewSummary(QueryTimeRange range) {
        BossStatisticsQueryService.BossSummaryResponse bossSummary = bossStatisticsQueryService.querySummary(range);
        MarketStatisticsQueryService.EconomySummaryResponse economySummary = marketStatisticsQueryService.queryEconomySummary(range);
        List<DashboardAlert> alerts = alertSummary(range).alerts();

        int duelAttempts = 0;
        int duelClears = 0;
        int extremeAttempts = 0;
        int extremeClears = 0;
        for (BossStatisticsQueryService.BossStatisticsRow row : bossSummary.bosses()) {
            int clears = (int) Math.round(row.attempts() * row.clearRate());
            if ("extreme".equals(row.category())) {
                extremeAttempts += row.attempts();
                extremeClears += clears;
            } else {
                duelAttempts += row.attempts();
                duelClears += clears;
            }
        }

        long inflow = 0L;
        long outflow = 0L;
        for (EconomyFlowRecord flow : dataStore.economyFlows()) {
            if (range != null && !range.contains(flow.occurredAt())) {
                continue;
            }
            if (flow.isInflow()) {
                inflow += flow.amount();
            } else if (flow.isOutflow()) {
                outflow += flow.amount();
            }
        }
        long dailyGoldNet = inflow - outflow;

        return new OverviewSummary(
                DATE_FORMAT.format(range == null ? java.time.Instant.now() : range.to()),
                dailyGoldNet,
                duelAttempts == 0 ? 0.0d : (double) duelClears / duelAttempts,
                extremeAttempts == 0 ? 0.0d : (double) extremeClears / extremeAttempts,
                economySummary.totalTradeVolume(),
                alerts.size()
        );
    }

    public BossStatisticsQueryService.BossSummaryResponse bossStatisticsSummary(QueryTimeRange range) {
        return bossStatisticsQueryService.querySummary(range);
    }

    public MarketStatisticsQueryService.EconomySummaryResponse economySummary(QueryTimeRange range) {
        return marketStatisticsQueryService.queryEconomySummary(range);
    }

    public LifeStatisticsQueryService.LifeSummaryResponse lifeSummary(QueryTimeRange range) {
        return lifeStatisticsQueryService.querySummary(range);
    }

    public AlertSummary alertSummary(QueryTimeRange range) {
        List<DashboardAlert> alerts = dataStore.alerts().stream()
                .filter(alert -> range == null || range.contains(alert.occurredAt()))
                .sorted(Comparator.comparing(DashboardAlert::occurredAt).reversed())
                .toList();

        long critical = alerts.stream().filter(alert -> "critical".equals(alert.level())).count();
        long warning = alerts.stream().filter(alert -> "warning".equals(alert.level())).count();
        long info = alerts.stream().filter(alert -> "info".equals(alert.level())).count();

        return new AlertSummary(range, (int) critical, (int) warning, (int) info, alerts);
    }

    public record OverviewSummary(
            String date,
            long dailyGoldNet,
            double duelBossClearRate,
            double extremeBossClearRate,
            long tradeVolume,
            int alertCount
    ) {
    }

    public record AlertSummary(
            QueryTimeRange range,
            int criticalCount,
            int warningCount,
            int infoCount,
            List<DashboardAlert> alerts
    ) {
    }
}
