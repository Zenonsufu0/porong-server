package kr.zenon.rpg.operations.query;

import kr.zenon.rpg.boss.db.BossSessionRepository;
import kr.zenon.rpg.operations.http.ZenonHttpServer;
import kr.zenon.rpg.operations.query.api.AdminApiEndpointLayer;
import kr.zenon.rpg.operations.query.discord.DiscordCommandQueryAdapter;
import kr.zenon.rpg.operations.query.service.AdminDashboardService;
import kr.zenon.rpg.operations.query.service.BossStatisticsQueryService;
import kr.zenon.rpg.operations.query.service.HallOfFameQueryService;
import kr.zenon.rpg.operations.query.service.LifeStatisticsQueryService;
import kr.zenon.rpg.operations.query.service.MarketStatisticsQueryService;
import kr.zenon.rpg.operations.query.service.PlayerDetailQueryService;
import kr.zenon.rpg.operations.query.service.PublicSnapshotQueryService;
import kr.zenon.rpg.operations.query.store.InMemoryOperationsDataStore;

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
        ZenonHttpServer httpServer,
        kr.zenon.rpg.auth.AuthService authService
) {
}
