package kr.zenon.rpg.boss.engine;

import kr.zenon.rpg.boss.db.BossSessionRepository;

public record BossEngineRuntime(
        BossRunService runService,
        BossEntryRuleRegistry entryRuleRegistry,
        BossPatternRegistry patternRegistry,
        InMemoryBossRunRecordHook runRecordHook,
        BossSessionRepository bossSessionRepository
) {
}
