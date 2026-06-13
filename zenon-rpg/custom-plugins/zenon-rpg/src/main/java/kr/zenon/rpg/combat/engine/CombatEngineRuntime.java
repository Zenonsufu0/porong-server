package kr.zenon.rpg.combat.engine;

public record CombatEngineRuntime(
        CombatEngine combatEngine,
        StateRegistry stateRegistry,
        BuffDebuffService buffDebuffService,
        StateApplier stateApplier,
        ResourceHandler resourceHandler,
        CombatFormulaResolver combatFormulaResolver,
        TagDamageResolver tagDamageResolver,
        ConditionalDamageResolver conditionalDamageResolver
) {
}
