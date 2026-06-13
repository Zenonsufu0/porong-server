package kr.zenon.rpg.life.engine;

public record LifeEngineRuntime(
        LifeGatherService gatherService,
        LifeCraftService craftService,
        LifeSkillProgressionService progressionService,
        EstateUnlockService estateUnlockService,
        EstateFacilityService estateFacilityService,
        EstateHarvestService estateHarvestService,
        EstateSnapshotBuilder estateSnapshotBuilder,
        LifeGatherNodeRegistry gatherNodeRegistry,
        LifeRecipeRegistry recipeRegistry,
        LifeSkillExpRegistry lifeSkillExpRegistry,
        EstateUnlockRuleRegistry estateUnlockRuleRegistry,
        EstateFacilityRegistry estateFacilityRegistry,
        EstateFacilityLevelRegistry estateFacilityLevelRegistry,
        InMemoryLifeCraftLogHook craftLogHook,
        InMemoryEstateHarvestLogHook harvestLogHook
) {
}
