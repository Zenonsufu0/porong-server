package com.poro.rpg.growth.engine;

public record GrowthEngineRuntime(
        EquipmentService equipmentService,
        EnhancementService enhancementService,
        PotentialService potentialService,
        SuccessionService successionService,
        SetBonusService setBonusService,
        RuneService runeService,
        EngravingService engravingService,
        PlayerGrowthSnapshotBuilder snapshotBuilder,
        EnhancementRuleRegistry enhancementRuleRegistry,
        PotentialOptionRegistry potentialOptionRegistry,
        SetBonusRegistry setBonusRegistry,
        RuneRegistry runeRegistry,
        EngravingRegistry engravingRegistry,
        InMemoryEnhancementLogHook enhancementLogHook,
        TraceSubstatRoller traceSubstatRoller
) {
}
