package kr.zenon.rpg.operations.query.service;

import kr.zenon.rpg.operations.query.model.MarketPricePoint;
import kr.zenon.rpg.operations.query.model.QueryTimeRange;
import kr.zenon.rpg.operations.query.store.OperationsDataStore;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public final class MarketStatisticsQueryService {
    private final OperationsDataStore dataStore;

    public MarketStatisticsQueryService(OperationsDataStore dataStore) {
        this.dataStore = Objects.requireNonNull(dataStore, "dataStore");
    }

    public EconomySummaryResponse queryEconomySummary(QueryTimeRange range) {
        List<MarketPricePoint> points = inRange(range);
        List<ItemMarketSummary> items = buildItemSummaries(points);

        long totalTradeVolume = items.stream().mapToLong(ItemMarketSummary::tradeVolume).sum();
        long totalSupply = items.stream().mapToLong(ItemMarketSummary::supplyVolume).sum();

        List<ItemMarketSummary> popularItems = items.stream()
                .sorted(Comparator.comparingLong(ItemMarketSummary::tradeVolume).reversed())
                .limit(5)
                .toList();
        List<ItemMarketSummary> risingItems = items.stream()
                .sorted(Comparator.comparingDouble(ItemMarketSummary::changeRate).reversed())
                .limit(5)
                .toList();
        List<ItemMarketSummary> fallingItems = items.stream()
                .sorted(Comparator.comparingDouble(ItemMarketSummary::changeRate))
                .limit(5)
                .toList();

        return new EconomySummaryResponse(
                range,
                totalTradeVolume,
                totalSupply,
                List.copyOf(items),
                popularItems,
                risingItems,
                fallingItems
        );
    }

    public MarketPriceSnapshot queryItemPrice(String itemId, QueryTimeRange range) {
        String normalizedItemId = normalize(itemId);
        List<MarketPricePoint> points = inRange(range).stream()
                .filter(point -> point.itemId().equals(normalizedItemId))
                .sorted(Comparator.comparing(MarketPricePoint::capturedAt))
                .toList();
        if (points.isEmpty()) {
            return new MarketPriceSnapshot(normalizedItemId, 0.0d, 0L, 0L, 0.0d);
        }

        MarketPricePoint latest = points.get(points.size() - 1);
        MarketPricePoint previous = points.size() >= 2 ? points.get(points.size() - 2) : latest;
        double changeRate = calculateChangeRate(previous.avgPrice(), latest.avgPrice());
        return new MarketPriceSnapshot(
                normalizedItemId,
                latest.avgPrice(),
                latest.tradeVolume(),
                latest.supplyVolume(),
                changeRate
        );
    }

    private List<MarketPricePoint> inRange(QueryTimeRange range) {
        if (range == null) {
            return dataStore.marketPriceHistory();
        }
        return dataStore.marketPriceHistory().stream()
                .filter(point -> range.contains(point.capturedAt()))
                .toList();
    }

    private List<ItemMarketSummary> buildItemSummaries(List<MarketPricePoint> points) {
        Map<String, List<MarketPricePoint>> grouped = points.stream()
                .collect(Collectors.groupingBy(MarketPricePoint::itemId, LinkedHashMap::new, Collectors.toList()));
        List<ItemMarketSummary> summaries = new ArrayList<>();
        for (Map.Entry<String, List<MarketPricePoint>> entry : grouped.entrySet()) {
            List<MarketPricePoint> sorted = entry.getValue().stream()
                    .sorted(Comparator.comparing(MarketPricePoint::capturedAt))
                    .toList();
            MarketPricePoint latest = sorted.get(sorted.size() - 1);
            MarketPricePoint previous = sorted.size() >= 2 ? sorted.get(sorted.size() - 2) : latest;
            summaries.add(new ItemMarketSummary(
                    entry.getKey(),
                    latest.avgPrice(),
                    latest.tradeVolume(),
                    latest.supplyVolume(),
                    calculateChangeRate(previous.avgPrice(), latest.avgPrice())
            ));
        }
        return List.copyOf(summaries);
    }

    private double calculateChangeRate(double previous, double latest) {
        if (previous <= 0.0d) {
            return 0.0d;
        }
        return (latest - previous) / previous;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    public record ItemMarketSummary(
            String itemId,
            double avgPrice,
            long tradeVolume,
            long supplyVolume,
            double changeRate
    ) {
    }

    public record EconomySummaryResponse(
            QueryTimeRange range,
            long totalTradeVolume,
            long totalSupplyVolume,
            List<ItemMarketSummary> items,
            List<ItemMarketSummary> popularItems,
            List<ItemMarketSummary> risingItems,
            List<ItemMarketSummary> fallingItems
    ) {
    }

    public record MarketPriceSnapshot(
            String itemId,
            double avgPrice,
            long tradeVolume,
            long supplyVolume,
            double changeRate
    ) {
    }
}
