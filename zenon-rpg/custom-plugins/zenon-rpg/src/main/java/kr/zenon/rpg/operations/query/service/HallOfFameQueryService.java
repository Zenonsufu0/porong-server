package kr.zenon.rpg.operations.query.service;

import kr.zenon.rpg.boss.engine.BossEngineRuntime;
import kr.zenon.rpg.boss.engine.BossResultSummary;
import kr.zenon.rpg.operations.query.model.QueryTimeRange;
import kr.zenon.rpg.operations.query.store.OperationsDataStore;
import kr.zenon.rpg.quest.engine.QuestAchievementRuntime;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class HallOfFameQueryService {
    private final QuestAchievementRuntime questAchievementRuntime;
    private final BossEngineRuntime bossEngineRuntime;
    private final OperationsDataStore dataStore;

    public HallOfFameQueryService(
            QuestAchievementRuntime questAchievementRuntime,
            BossEngineRuntime bossEngineRuntime,
            OperationsDataStore dataStore
    ) {
        this.questAchievementRuntime = Objects.requireNonNull(questAchievementRuntime, "questAchievementRuntime");
        this.bossEngineRuntime = Objects.requireNonNull(bossEngineRuntime, "bossEngineRuntime");
        this.dataStore = Objects.requireNonNull(dataStore, "dataStore");
    }

    public HallOfFameSnapshot query(QueryTimeRange range) {
        List<InMemoryEntry> serverFirst = new ArrayList<>();
        List<InMemoryEntry> extreme = new ArrayList<>();
        int order = 0;
        for (kr.zenon.rpg.quest.engine.InMemoryHallOfFameHook.HallOfFameEntry entry : questAchievementRuntime.hallOfFameHook().entries()) {
            order++;
            InMemoryEntry converted = new InMemoryEntry(order, entry.userId(), entry.achievementId(), entry.recordId(), entry.amount());
            serverFirst.add(converted);
            if (entry.recordId().contains("extreme") || entry.achievementId().contains("extreme")) {
                extreme.add(converted);
            }
        }

        List<SeasonBestRecord> seasonBest = allBossSummaries().stream()
                .filter(BossResultSummary::clearSuccess)
                .sorted(Comparator.comparingLong(BossResultSummary::clearTimeSeconds))
                .limit(10)
                .map(summary -> new SeasonBestRecord(
                        summary.bossId(),
                        summary.runId(),
                        summary.clearTimeSeconds(),
                        summary.remainingDeathCount()
                ))
                .toList();

        List<NoDeathRecord> noDeath = allBossSummaries().stream()
                .filter(BossResultSummary::clearSuccess)
                .filter(this::isNoDeath)
                .map(summary -> new NoDeathRecord(
                        summary.bossId(),
                        summary.runId(),
                        summary.clearTimeSeconds()
                ))
                .limit(10)
                .toList();

        List<String> transcendenceHolders = dataStore.transcendenceHolders().stream().sorted().toList();

        return new HallOfFameSnapshot(
                range,
                serverFirst,
                extreme,
                seasonBest,
                noDeath,
                transcendenceHolders
        );
    }

    private List<BossResultSummary> allBossSummaries() {
        List<BossResultSummary> merged = new ArrayList<>();
        merged.addAll(bossEngineRuntime.runRecordHook().summaries());
        merged.addAll(dataStore.additionalBossSummaries());
        return List.copyOf(merged);
    }

    private boolean isNoDeath(BossResultSummary summary) {
        for (Map<String, Object> participant : summary.participantSummaryPlaceholder()) {
            Object raw = participant.get("deaths");
            if (raw instanceof Number number && number.intValue() > 0) {
                return false;
            }
        }
        return true;
    }

    public record InMemoryEntry(
            int order,
            String userId,
            String achievementId,
            String recordId,
            long amount
    ) {
    }

    public record SeasonBestRecord(
            String bossId,
            String runId,
            long clearTimeSeconds,
            int remainingDeathCount
    ) {
    }

    public record NoDeathRecord(
            String bossId,
            String runId,
            long clearTimeSeconds
    ) {
    }

    public record HallOfFameSnapshot(
            QueryTimeRange range,
            List<InMemoryEntry> serverFirst,
            List<InMemoryEntry> extremeRecords,
            List<SeasonBestRecord> seasonBest,
            List<NoDeathRecord> noDeathRecords,
            List<String> transcendenceHolders
    ) {
    }
}
