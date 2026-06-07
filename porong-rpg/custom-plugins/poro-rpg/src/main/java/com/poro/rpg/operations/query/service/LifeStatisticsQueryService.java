package com.poro.rpg.operations.query.service;

import com.poro.rpg.life.engine.EstateHarvestLogEntry;
import com.poro.rpg.life.engine.LifeCraftLogEntry;
import com.poro.rpg.life.engine.LifeEngineRuntime;
import com.poro.rpg.operations.query.model.ConsumableUsageRecord;
import com.poro.rpg.operations.query.model.LifeResourceSupplyRecord;
import com.poro.rpg.operations.query.model.QueryTimeRange;
import com.poro.rpg.operations.query.store.OperationsDataStore;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public final class LifeStatisticsQueryService {
    private final LifeEngineRuntime lifeEngineRuntime;
    private final OperationsDataStore dataStore;

    public LifeStatisticsQueryService(LifeEngineRuntime lifeEngineRuntime, OperationsDataStore dataStore) {
        this.lifeEngineRuntime = Objects.requireNonNull(lifeEngineRuntime, "lifeEngineRuntime");
        this.dataStore = Objects.requireNonNull(dataStore, "dataStore");
    }

    public LifeSummaryResponse querySummary(QueryTimeRange range) {
        Map<String, Long> totalSupplyByItem = new LinkedHashMap<>();
        long estateSupplyTotal = 0L;
        long totalSupply = 0L;

        for (LifeResourceSupplyRecord record : dataStore.lifeResourceSupplies()) {
            if (!contains(range, record.occurredAt())) {
                continue;
            }
            merge(totalSupplyByItem, record.itemId(), record.amount());
            totalSupply += record.amount();
            if ("estate".equals(record.sourceType())) {
                estateSupplyTotal += record.amount();
            }
        }

        for (EstateHarvestLogEntry harvest : lifeEngineRuntime.harvestLogHook().entries()) {
            if (!contains(range, harvest.harvestedAt())) {
                continue;
            }
            for (Map.Entry<String, Long> item : harvest.harvestedItems().entrySet()) {
                long amount = Math.max(0L, item.getValue());
                merge(totalSupplyByItem, item.getKey(), amount);
                totalSupply += amount;
                estateSupplyTotal += amount;
            }
        }

        List<ResourceSupplyStat> topResources = totalSupplyByItem.entrySet().stream()
                .map(entry -> new ResourceSupplyStat(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparingLong(ResourceSupplyStat::supply).reversed())
                .limit(10)
                .toList();

        Map<String, Long> consumableUsage = new LinkedHashMap<>();
        for (ConsumableUsageRecord usage : dataStore.consumableUsages()) {
            if (!contains(range, usage.occurredAt())) {
                continue;
            }
            merge(consumableUsage, usage.itemId(), usage.useCount());
        }

        for (LifeCraftLogEntry craftLog : lifeEngineRuntime.craftLogHook().entries()) {
            if (!contains(range, craftLog.craftedAt())) {
                continue;
            }
            String itemId = normalize(craftLog.resultItemId());
            if (itemId.contains("feast") || itemId.contains("potion")) {
                merge(consumableUsage, itemId, craftLog.resultAmount());
            }
        }

        List<ConsumableUsageStat> topConsumables = consumableUsage.entrySet().stream()
                .map(entry -> new ConsumableUsageStat(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparingLong(ConsumableUsageStat::uses).reversed())
                .limit(10)
                .toList();

        Map<Integer, Long> levelDistribution = new LinkedHashMap<>();
        for (Map<String, Integer> userLevels : dataStore.allLifeLevels().values()) {
            for (Integer level : userLevels.values()) {
                int normalizedLevel = Math.max(1, level == null ? 1 : level);
                levelDistribution.merge(normalizedLevel, 1L, Long::sum);
            }
        }
        List<LifeLevelDistribution> distributions = levelDistribution.entrySet().stream()
                .map(entry -> new LifeLevelDistribution(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparingInt(LifeLevelDistribution::level))
                .toList();

        double estateRatio = totalSupply == 0L ? 0.0d : (double) estateSupplyTotal / totalSupply;
        return new LifeSummaryResponse(
                range,
                totalSupply,
                estateSupplyTotal,
                estateRatio,
                List.copyOf(topResources),
                List.copyOf(topConsumables),
                List.copyOf(distributions)
        );
    }

    private boolean contains(QueryTimeRange range, java.time.Instant instant) {
        return range == null || range.contains(instant);
    }

    private void merge(Map<String, Long> map, String itemId, long amount) {
        String normalized = normalize(itemId);
        if (normalized.isBlank() || amount <= 0L) {
            return;
        }
        map.merge(normalized, amount, Long::sum);
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    public record ResourceSupplyStat(
            String itemId,
            long supply
    ) {
    }

    public record ConsumableUsageStat(
            String itemId,
            long uses
    ) {
    }

    public record LifeLevelDistribution(
            int level,
            long users
    ) {
    }

    public record LifeSummaryResponse(
            QueryTimeRange range,
            long totalSupply,
            long estateSupply,
            double estateSupplyRatio,
            List<ResourceSupplyStat> topResources,
            List<ConsumableUsageStat> topConsumables,
            List<LifeLevelDistribution> levelDistribution
    ) {
    }
}
