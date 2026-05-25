package com.poro.empire.boss.engine;

import com.poro.empire.boss.db.BossSessionRepository;

public record BossEngineRuntime(
        BossRunService runService,
        BossEntryRuleRegistry entryRuleRegistry,
        BossPatternRegistry patternRegistry,
        InMemoryBossRunRecordHook runRecordHook,
        BossSessionRepository bossSessionRepository
) {
}
