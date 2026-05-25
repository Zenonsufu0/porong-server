package com.poro.empire.operations.query;

import com.poro.empire.boss.db.BossSessionRepository;
import com.poro.empire.operations.http.EmpireHttpServer;
import com.poro.empire.operations.query.api.AdminApiEndpointLayer;
import com.poro.empire.operations.query.discord.DiscordCommandQueryAdapter;
import com.poro.empire.operations.query.service.AdminDashboardService;
import com.poro.empire.operations.query.service.BossStatisticsQueryService;
import com.poro.empire.operations.query.service.HallOfFameQueryService;
import com.poro.empire.operations.query.service.LifeStatisticsQueryService;
import com.poro.empire.operations.query.service.MarketStatisticsQueryService;
import com.poro.empire.operations.query.service.PlayerDetailQueryService;
import com.poro.empire.operations.query.service.PublicSnapshotQueryService;
import com.poro.empire.operations.query.store.InMemoryOperationsDataStore;

public record OperationsQueryRuntime(
        InMemoryOperationsDataStore dataStore,
        AdminDashboardService adminDashboardService,
        PlayerDetailQueryService playerDetailQueryService,
        BossStatisticsQueryService bossStatisticsQueryService,
        MarketStatisticsQueryService marketStatisticsQueryService,
        LifeStatisticsQueryService lifeStatisticsQueryService,
        HallOfFameQueryService hallOfFameQueryService,
        PublicSnapshotQueryService publicSnapshotQueryService,
        AdminApiEndpointLayer adminApiEndpointLayer,
        DiscordCommandQueryAdapter discordCommandQueryAdapter,
        BossSessionRepository bossSessionRepository,
        EmpireHttpServer httpServer
) {
}
