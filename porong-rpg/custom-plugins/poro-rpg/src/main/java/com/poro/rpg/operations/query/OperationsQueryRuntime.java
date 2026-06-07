package com.poro.rpg.operations.query;

import com.poro.rpg.boss.db.BossSessionRepository;
import com.poro.rpg.operations.http.PoroHttpServer;
import com.poro.rpg.operations.query.api.AdminApiEndpointLayer;
import com.poro.rpg.operations.query.discord.DiscordCommandQueryAdapter;
import com.poro.rpg.operations.query.service.AdminDashboardService;
import com.poro.rpg.operations.query.service.BossStatisticsQueryService;
import com.poro.rpg.operations.query.service.HallOfFameQueryService;
import com.poro.rpg.operations.query.service.LifeStatisticsQueryService;
import com.poro.rpg.operations.query.service.MarketStatisticsQueryService;
import com.poro.rpg.operations.query.service.PlayerDetailQueryService;
import com.poro.rpg.operations.query.service.PublicSnapshotQueryService;
import com.poro.rpg.operations.query.store.InMemoryOperationsDataStore;

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
        PoroHttpServer httpServer
) {
}
