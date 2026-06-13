package kr.zenon.rpg.operations.query.api;

public final class AdminApiEndpoints {
    public static final String DASHBOARD_OVERVIEW = "/admin/dashboard/overview";
    public static final String DASHBOARD_BOSSES = "/admin/dashboard/bosses";
    public static final String DASHBOARD_ECONOMY = "/admin/dashboard/economy";
    public static final String DASHBOARD_LIFE = "/admin/dashboard/life";
    public static final String PLAYER_DETAIL = "/admin/players/{userId}";
    public static final String ALERTS = "/admin/alerts";

    private AdminApiEndpoints() {
    }
}
