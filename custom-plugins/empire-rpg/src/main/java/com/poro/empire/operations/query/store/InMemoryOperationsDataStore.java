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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class InMemoryOperationsDataStore implements OperationsDataStore {
    private final Map<String, PlayerProfileRecord> profiles = new ConcurrentHashMap<>();
    private final Map<String, PlayerGrowthSnapshotBuilder.PlayerGrowthSnapshot> growthSnapshots = new ConcurrentHashMap<>();
    private final Map<String, EstateSnapshotBuilder.EstateSnapshot> lifeSnapshots = new ConcurrentHashMap<>();
    private final Map<String, PlayerQuestAndHonorSnapshotBuilder.Snapshot> questSnapshots = new ConcurrentHashMap<>();
    private final Map<String, Map<String, Integer>> lifeLevels = new ConcurrentHashMap<>();

    private final List<BossResultSummary> additionalBossSummaries = java.util.Collections.synchronizedList(new ArrayList<>());
    private final List<MarketPricePoint> marketPriceHistory = java.util.Collections.synchronizedList(new ArrayList<>());
    private final List<EconomyFlowRecord> economyFlows = java.util.Collections.synchronizedList(new ArrayList<>());
    private final List<LifeResourceSupplyRecord> lifeResourceSupplies = java.util.Collections.synchronizedList(new ArrayList<>());
    private final List<ConsumableUsageRecord> consumableUsages = java.util.Collections.synchronizedList(new ArrayList<>());
    private final List<DashboardAlert> alerts = java.util.Collections.synchronizedList(new ArrayList<>());
    private final Set<String> transcendenceHolders = java.util.Collections.synchronizedSet(new LinkedHashSet<>());

    @Override
    public void upsertPlayerProfile(PlayerProfileRecord profile) {
        profiles.put(normalize(profile.userId()), profile);
    }

    @Override
    public Optional<PlayerProfileRecord> playerProfile(String userId) {
        return Optional.ofNullable(profiles.get(normalize(userId)));
    }

    @Override
    public Optional<PlayerProfileRecord> playerProfileByNick(String nickname) {
        String norm = normalize(nickname);
        return profiles.values().stream()
                .filter(p -> normalize(p.nickname()).equals(norm))
                .findFirst();
    }

    @Override
    public void upsertGrowthSnapshot(String userId, PlayerGrowthSnapshotBuilder.PlayerGrowthSnapshot snapshot) {
        growthSnapshots.put(normalize(userId), snapshot);
    }

    @Override
    public Optional<PlayerGrowthSnapshotBuilder.PlayerGrowthSnapshot> growthSnapshot(String userId) {
        return Optional.ofNullable(growthSnapshots.get(normalize(userId)));
    }

    @Override
    public void upsertLifeSnapshot(String userId, EstateSnapshotBuilder.EstateSnapshot snapshot) {
        lifeSnapshots.put(normalize(userId), snapshot);
    }

    @Override
    public Optional<EstateSnapshotBuilder.EstateSnapshot> lifeSnapshot(String userId) {
        return Optional.ofNullable(lifeSnapshots.get(normalize(userId)));
    }

    @Override
    public void upsertQuestSnapshot(String userId, PlayerQuestAndHonorSnapshotBuilder.Snapshot snapshot) {
        questSnapshots.put(normalize(userId), snapshot);
    }

    @Override
    public Optional<PlayerQuestAndHonorSnapshotBuilder.Snapshot> questSnapshot(String userId) {
        return Optional.ofNullable(questSnapshots.get(normalize(userId)));
    }

    @Override
    public void upsertLifeLevels(String userId, Map<String, Integer> lifeLevels) {
        Map<String, Integer> normalized = new LinkedHashMap<>();
        if (lifeLevels != null) {
            for (Map.Entry<String, Integer> entry : lifeLevels.entrySet()) {
                normalized.put(normalize(entry.getKey()), Math.max(1, entry.getValue() == null ? 1 : entry.getValue()));
            }
        }
        this.lifeLevels.put(normalize(userId), Map.copyOf(normalized));
    }

    @Override
    public Map<String, Integer> lifeLevels(String userId) {
        return lifeLevels.getOrDefault(normalize(userId), Map.of());
    }

    @Override
    public Map<String, Map<String, Integer>> allLifeLevels() {
        return Map.copyOf(new LinkedHashMap<>(lifeLevels));
    }

    @Override
    public void addBossSummary(BossResultSummary summary) {
        additionalBossSummaries.add(summary);
    }

    @Override
    public List<BossResultSummary> additionalBossSummaries() {
        synchronized (additionalBossSummaries) {
            return List.copyOf(additionalBossSummaries);
        }
    }

    @Override
    public void addMarketPricePoint(MarketPricePoint point) {
        marketPriceHistory.add(point);
    }

    @Override
    public List<MarketPricePoint> marketPriceHistory() {
        synchronized (marketPriceHistory) {
            return List.copyOf(marketPriceHistory);
        }
    }

    @Override
    public void addEconomyFlow(EconomyFlowRecord record) {
        economyFlows.add(record);
    }

    @Override
    public List<EconomyFlowRecord> economyFlows() {
        synchronized (economyFlows) {
            return List.copyOf(economyFlows);
        }
    }

    @Override
    public void addLifeResourceSupply(LifeResourceSupplyRecord record) {
        lifeResourceSupplies.add(record);
    }

    @Override
    public List<LifeResourceSupplyRecord> lifeResourceSupplies() {
        synchronized (lifeResourceSupplies) {
            return List.copyOf(lifeResourceSupplies);
        }
    }

    @Override
    public void addConsumableUsage(ConsumableUsageRecord record) {
        consumableUsages.add(record);
    }

    @Override
    public List<ConsumableUsageRecord> consumableUsages() {
        synchronized (consumableUsages) {
            return List.copyOf(consumableUsages);
        }
    }

    @Override
    public void addAlert(DashboardAlert alert) {
        alerts.add(alert);
    }

    @Override
    public List<DashboardAlert> alerts() {
        synchronized (alerts) {
            return List.copyOf(alerts);
        }
    }

    @Override
    public void markTranscendenceHolder(String userId) {
        transcendenceHolders.add(normalize(userId));
    }

    @Override
    public Set<String> transcendenceHolders() {
        synchronized (transcendenceHolders) {
            return Set.copyOf(new LinkedHashSet<>(transcendenceHolders));
        }
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
