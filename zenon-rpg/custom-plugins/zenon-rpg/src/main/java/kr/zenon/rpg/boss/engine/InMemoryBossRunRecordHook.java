package kr.zenon.rpg.boss.engine;

import java.util.ArrayList;
import java.util.List;

public final class InMemoryBossRunRecordHook implements BossRunRecordHook {
    private final List<String> startedRunIds = new ArrayList<>();
    private final List<BossResultSummary> summaries = new ArrayList<>();

    @Override
    public void onRunStarted(BossRun run) {
        startedRunIds.add(run.runId());
    }

    @Override
    public void onRunEnded(BossResultSummary summary) {
        summaries.add(summary);
    }

    public List<String> startedRunIds() {
        return List.copyOf(startedRunIds);
    }

    public List<BossResultSummary> summaries() {
        return List.copyOf(summaries);
    }
}
