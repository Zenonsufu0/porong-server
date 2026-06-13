package kr.zenon.rpg.operations.query.api;

import kr.zenon.rpg.operations.query.model.QueryTimeRange;
import kr.zenon.rpg.operations.query.service.AdminDashboardService;
import kr.zenon.rpg.operations.query.service.PlayerDetailQueryService;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public final class AdminApiEndpointLayer {
    private final AdminDashboardService adminDashboardService;
    private final PlayerDetailQueryService playerDetailQueryService;

    public AdminApiEndpointLayer(
            AdminDashboardService adminDashboardService,
            PlayerDetailQueryService playerDetailQueryService
    ) {
        this.adminDashboardService = Objects.requireNonNull(adminDashboardService, "adminDashboardService");
        this.playerDetailQueryService = Objects.requireNonNull(playerDetailQueryService, "playerDetailQueryService");
    }

    public Object getOverview(QueryTimeRange range) {
        return adminDashboardService.overviewSummary(range);
    }

    public Object getBosses(QueryTimeRange range) {
        return adminDashboardService.bossStatisticsSummary(range);
    }

    public Object getEconomy(QueryTimeRange range) {
        return adminDashboardService.economySummary(range);
    }

    public Object getLife(QueryTimeRange range) {
        return adminDashboardService.lifeSummary(range);
    }

    public Object getPlayerDetail(String userId) {
        return playerDetailQueryService.query(userId);
    }

    public Object getAlerts(QueryTimeRange range) {
        return adminDashboardService.alertSummary(range);
    }

    public Map<String, Object> listEndpoints() {
        Map<String, Object> endpoints = new LinkedHashMap<>();
        endpoints.put("GET", java.util.List.of(
                AdminApiEndpoints.DASHBOARD_OVERVIEW,
                AdminApiEndpoints.DASHBOARD_BOSSES,
                AdminApiEndpoints.DASHBOARD_ECONOMY,
                AdminApiEndpoints.DASHBOARD_LIFE,
                AdminApiEndpoints.PLAYER_DETAIL,
                AdminApiEndpoints.ALERTS
        ));
        return Map.copyOf(endpoints);
    }
}
