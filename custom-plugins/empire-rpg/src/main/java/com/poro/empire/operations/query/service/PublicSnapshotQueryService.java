package com.poro.empire.operations.query.service;

import com.poro.empire.operations.query.model.QueryTimeRange;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class PublicSnapshotQueryService {
    private final PlayerDetailQueryService playerDetailQueryService;
    private final MarketStatisticsQueryService marketStatisticsQueryService;
    private final HallOfFameQueryService hallOfFameQueryService;

    public PublicSnapshotQueryService(
            PlayerDetailQueryService playerDetailQueryService,
            MarketStatisticsQueryService marketStatisticsQueryService,
            HallOfFameQueryService hallOfFameQueryService
    ) {
        this.playerDetailQueryService = Objects.requireNonNull(playerDetailQueryService, "playerDetailQueryService");
        this.marketStatisticsQueryService = Objects.requireNonNull(marketStatisticsQueryService, "marketStatisticsQueryService");
        this.hallOfFameQueryService = Objects.requireNonNull(hallOfFameQueryService, "hallOfFameQueryService");
    }

    public PlayerDetailQueryService.PlayerSnapshotProjection playerSnapshot(String userId) {
        return playerDetailQueryService.playerSnapshot(userId);
    }

    public PlayerDetailQueryService.EquipmentProjection equipmentSnapshot(String userId) {
        return playerDetailQueryService.equipmentSnapshot(userId);
    }

    public List<PlayerDetailQueryService.PlayerBossRecordProjection> bossRecordsSnapshot(String userId) {
        return playerDetailQueryService.bossRecordsSnapshot(userId);
    }

    public PlayerDetailQueryService.LifeProjection lifeSnapshot(String userId) {
        return playerDetailQueryService.lifeSnapshot(userId);
    }

    public MarketStatisticsQueryService.MarketPriceSnapshot marketPriceSnapshot(String itemId, QueryTimeRange range) {
        return marketStatisticsQueryService.queryItemPrice(itemId, range);
    }

    public HallOfFameQueryService.HallOfFameSnapshot hallOfFameSnapshot(QueryTimeRange range) {
        return hallOfFameQueryService.query(range);
    }

    public Optional<PlayerDetailQueryService.PlayerSnapshotProjection> playerSnapshotByNick(String nick) {
        return playerDetailQueryService.playerSnapshotByNick(nick);
    }

    public Optional<PlayerDetailQueryService.LifeProjection> lifeSnapshotByNick(String nick) {
        return playerDetailQueryService.lifeSnapshotByNick(nick);
    }

    public Optional<List<PlayerDetailQueryService.PlayerBossRecordProjection>> bossRecordsSnapshotByNick(String nick) {
        return playerDetailQueryService.bossRecordsSnapshotByNick(nick);
    }
}
