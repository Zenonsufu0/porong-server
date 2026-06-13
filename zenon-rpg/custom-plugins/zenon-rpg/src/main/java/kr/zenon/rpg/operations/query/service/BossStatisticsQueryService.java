package kr.zenon.rpg.operations.query.service;

import kr.zenon.rpg.boss.engine.BossEngineRuntime;
import kr.zenon.rpg.boss.engine.BossResultSummary;
import kr.zenon.rpg.common.registry.master.BossMasterRegistry;
import kr.zenon.rpg.common.registry.master.model.BossMaster;
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

public final class BossStatisticsQueryService {
    private final BossMasterRegistry bossMasterRegistry;
    private final BossEngineRuntime bossEngineRuntime;
    private final OperationsDataStore dataStore;

    public BossStatisticsQueryService(
            BossMasterRegistry bossMasterRegistry,
            BossEngineRuntime bossEngineRuntime,
            OperationsDataStore dataStore
    ) {
        this.bossMasterRegistry = Objects.requireNonNull(bossMasterRegistry, "bossMasterRegistry");
        this.bossEngineRuntime = Objects.requireNonNull(bossEngineRuntime, "bossEngineRuntime");
        this.dataStore = Objects.requireNonNull(dataStore, "dataStore");
    }

    public BossSummaryResponse querySummary(QueryTimeRange range) {
        List<BossResultSummary> source = allSummaries();
        List<BossResultSummary> inRange = source.stream()
                .filter(summary -> isInRange(summary, range))
                .toList();

        Map<String, List<BossResultSummary>> grouped = inRange.stream()
                .collect(Collectors.groupingBy(summary -> normalize(summary.bossId()), LinkedHashMap::new, Collectors.toList()));

        List<BossStatisticsRow> rows = new ArrayList<>();
        int totalAttempts = 0;
        int totalClears = 0;
        for (Map.Entry<String, List<BossResultSummary>> entry : grouped.entrySet()) {
            String bossId = entry.getKey();
            BossMaster boss = bossMasterRegistry.find(bossId).orElse(null);
            List<BossResultSummary> summaries = entry.getValue();
            int attempts = summaries.size();
            int clears = (int) summaries.stream().filter(BossResultSummary::clearSuccess).count();
            double clearRate = attempts == 0 ? 0.0d : (double) clears / attempts;

            double avgClearTime = summaries.stream()
                    .filter(BossResultSummary::clearSuccess)
                    .mapToLong(BossResultSummary::clearTimeSeconds)
                    .average()
                    .orElse(0.0d);

            double avgDeaths = summaries.stream()
                    .mapToInt(this::totalDeaths)
                    .average()
                    .orElse(0.0d);

            Map<Integer, Long> phaseCounts = summaries.stream()
                    .collect(Collectors.groupingBy(BossResultSummary::phaseReached, LinkedHashMap::new, Collectors.counting()));
            List<PhaseReachStat> phaseStats = phaseCounts.entrySet().stream()
                    .map(phase -> new PhaseReachStat(
                            phase.getKey(),
                            phase.getValue(),
                            attempts == 0 ? 0.0d : (double) phase.getValue() / attempts
                    ))
                    .sorted(Comparator.comparingInt(PhaseReachStat::phase))
                    .toList();

            rows.add(new BossStatisticsRow(
                    bossId,
                    boss == null ? "" : boss.bossName(),
                    boss == null ? "duel" : (boss.extreme() ? "extreme" : "duel"),
                    attempts,
                    clearRate,
                    avgClearTime,
                    avgDeaths,
                    phaseStats
            ));

            totalAttempts += attempts;
            totalClears += clears;
        }

        rows.sort(Comparator.comparing(BossStatisticsRow::bossId));
        return new BossSummaryResponse(
                range,
                totalAttempts,
                totalClears,
                totalAttempts == 0 ? 0.0d : (double) totalClears / totalAttempts,
                List.copyOf(rows)
        );
    }

    private List<BossResultSummary> allSummaries() {
        List<BossResultSummary> merged = new ArrayList<>();
        if (bossEngineRuntime.runRecordHook() != null) {
            merged.addAll(bossEngineRuntime.runRecordHook().summaries());
        }
        merged.addAll(dataStore.additionalBossSummaries());
        return List.copyOf(merged);
    }

    private boolean isInRange(BossResultSummary summary, QueryTimeRange range) {
        // in-memory query: always include all summaries (range filtering not yet implemented)
        return true;
    }

    private int totalDeaths(BossResultSummary summary) {
        int total = 0;
        for (Map<String, Object> participant : summary.participantSummaryPlaceholder()) {
            Object raw = participant.get("deaths");
            if (raw instanceof Number number) {
                total += Math.max(0, number.intValue());
            }
        }
        return total;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    public record PhaseReachStat(
            int phase,
            long count,
            double reachRate
    ) {
    }

    public record BossStatisticsRow(
            String bossId,
            String bossName,
            String category,
            int attempts,
            double clearRate,
            double avgClearTimeSeconds,
            double avgDeaths,
            List<PhaseReachStat> phaseStats
    ) {
    }

    public record BossSummaryResponse(
            QueryTimeRange range,
            int totalAttempts,
            int totalClears,
            double totalClearRate,
            List<BossStatisticsRow> bosses
    ) {
    }
}
