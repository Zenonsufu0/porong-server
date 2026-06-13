package kr.zenon.rpg.boss.engine;

public interface BossRunRecordHook {
    void onRunStarted(BossRun run);

    void onRunEnded(BossResultSummary summary);
}
