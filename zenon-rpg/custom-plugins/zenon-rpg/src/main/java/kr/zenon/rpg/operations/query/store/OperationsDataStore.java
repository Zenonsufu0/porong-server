package kr.zenon.rpg.operations.query.store;

import kr.zenon.rpg.boss.engine.BossResultSummary;
import kr.zenon.rpg.growth.engine.PlayerGrowthSnapshotBuilder;
import kr.zenon.rpg.life.engine.EstateSnapshotBuilder;
import kr.zenon.rpg.operations.query.model.ConsumableUsageRecord;
import kr.zenon.rpg.operations.query.model.DashboardAlert;
import kr.zenon.rpg.operations.query.model.EconomyFlowRecord;
import kr.zenon.rpg.operations.query.model.LifeResourceSupplyRecord;
import kr.zenon.rpg.operations.query.model.MarketPricePoint;
import kr.zenon.rpg.operations.query.model.PlayerProfileRecord;
import kr.zenon.rpg.quest.engine.PlayerQuestAndHonorSnapshotBuilder;

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
