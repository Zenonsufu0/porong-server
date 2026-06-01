package com.poro.rpg.boss.engine;

import com.poro.rpg.boss.db.BossSessionRepository;

public record BossEngineRuntime(
        BossRunService runService,
        BossEntryRuleRegistry entryRuleRegistry,
        BossPatternRegistry patternRegistry,
        InMemoryBossRunRecordHook runRecordHook,
        BossSessionRepository bossSessionRepository
) {
}
