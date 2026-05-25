package com.poro.empire.boss.engine;

import java.util.List;
import java.util.Objects;

public final class CompositeBossRunRecordHook implements BossRunRecordHook {
    private final List<BossRunRecordHook> delegates;

    public CompositeBossRunRecordHook(List<BossRunRecordHook> delegates) {
        this.delegates = List.copyOf(Objects.requireNonNull(delegates, "delegates"));
    }

    @Override
    public void onRunStarted(BossRun run) {
        for (BossRunRecordHook hook : delegates) hook.onRunStarted(run);
    }

    @Override
    public void onRunEnded(BossResultSummary summary) {
        for (BossRunRecordHook hook : delegates) hook.onRunEnded(summary);
    }
}
