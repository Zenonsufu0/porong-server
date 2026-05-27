package com.poro.empire.operations.query.store;

import com.poro.empire.boss.engine.BossResultSummary;
import com.poro.empire.growth.engine.PlayerGrowthSnapshotBuilder;
import com.poro.empire.life.engine.EstateSnapshotBuilder;
import com.poro.empire.operations.query.model.ConsumableUsageRecord;
import com.poro.empire.operations.query.model.DashboardAlert;
import com.poro.empire.operations.query.model.EconomyFlowRecord;
import com.poro.empire.operations.query.model.LifeResourceSupplyRecord;
import com.poro.empire.operations.query.model.MarketPricePoint;
import com.poro.empire.operations.query.model.PlayerProfileRecord;
import com.poro.empire.quest.engine.PlayerQuestAndHonorSnapshotBuilder;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public interface OperationsDataStore {
    void upsertPlayerProfile(PlayerProfileRecord profile);

    Optional<PlayerProfileRecord> playerProfile(String userId);

    Optional<PlayerProfileRecord> playerProfileByNick(String nickname);

    void upsertGrowthSnapshot(String userId, PlayerGrowthSnapshotBuilder.PlayerGrowthSnapshot snapshot);

    Optional<PlayerGrowthSnapshotBuilder.PlayerGrowthSnapshot> growthSnapshot(String userId);

    void upsertLifeSnapshot(String userId, EstateSnapshotBuilder.EstateSnapshot snapshot);

    Optional<EstateSnapshotBuilder.EstateSnapshot> lifeSnapshot(String userId);

    void upsertQuestSnapshot(String userId, PlayerQuestAndHonorSnapshotBuilder.Snapshot snapshot);

    Optional<PlayerQuestAndHonorSnapshotBuilder.Snapshot> questSnapshot(String userId);

    void upsertLifeLevels(String userId, Map<String, Integer> lifeLevels);

    Map<String, Integer> lifeLevels(String userId);

    Map<String, Map<String, Integer>> allLifeLevels();

    void addBossSummary(BossResultSummary summary);

    List<BossResultSummary> additionalBossSummaries();

    void addMarketPricePoint(MarketPricePoint point);

    List<MarketPricePoint> marketPriceHistory();

    void addEconomyFlow(EconomyFlowRecord record);

    List<EconomyFlowRecord> economyFlows();

    void addLifeResourceSupply(LifeResourceSupplyRecord record);

    List<LifeResourceSupplyRecord> lifeResourceSupplies();

    void addConsumableUsage(ConsumableUsageRecord record);

    List<ConsumableUsageRecord> consumableUsages();

    void addAlert(DashboardAlert alert);

    List<DashboardAlert> alerts();

    void markTranscendenceHolder(String userId);

    Set<String> transcendenceHolders();
}
