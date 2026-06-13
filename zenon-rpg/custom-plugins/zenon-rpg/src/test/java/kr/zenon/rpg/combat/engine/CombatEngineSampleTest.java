package kr.zenon.rpg.combat.engine;

import kr.zenon.rpg.common.registry.master.SkillMasterRegistry;
import kr.zenon.rpg.common.registry.master.model.SkillMaster;
import kr.zenon.rpg.common.result.Result;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

class CombatEngineSampleTest {
    @Test
    void shouldResolveSamplePveDamageFlow() {
        SkillMasterRegistry skillRegistry = new SkillMasterRegistry();
        skillRegistry.register(new SkillMaster(
                "gs_break_slash", "warrior", "greatsword", "Break Slash",
                "builder", "core", "precision",
                120, 6, "crack", 1, 0,
                "NONE", "0", "crack"
        ));
        skillRegistry.register(new SkillMaster(
                "katana_draw_plus", "warrior", "katana", "Draw Plus",
                "spender", "precision", "core",
                240, 12, "sword_intent", 0, 3,
                "BONUS_VS_MARK", "12", "exposed_weakness"
        ));
        skillRegistry.register(new SkillMaster(
                "dagger_heart_cut", "assassin", "dagger", "Heart Cut",
                "finisher", "precision", "combo",
                325, 18, "shadow_mark", 1, 1,
                "BONUS_VS_MARK", "20", "shadow_mark"
        ));
        skillRegistry.register(new SkillMaster(
                "wand_dimension_break", "mage", "wand", "Dimension Break",
                "finisher", "core", "aoe",
                330, 18, "resonance", 0, 4,
                "BONUS_VS_STATUS", "15", "burn"
        ));

        StateRegistry stateRegistry = new StateRegistry();
        registerState(stateRegistry, "crack", "DEBUFF_MARK", true, 5);
        registerState(stateRegistry, "target_mark", "DEBUFF_MARK", true, 5);
        registerState(stateRegistry, "shadow_mark", "DEBUFF_MARK", true, 5);
        registerState(stateRegistry, "exposed_weakness", "DEBUFF_MARK", true, 3);
        registerState(stateRegistry, "burn", "DEBUFF_STATUS", true, 5);
        registerState(stateRegistry, "damage_reduction", "BUFF", true, 5);

        BuffDebuffService buffDebuffService = new BuffDebuffService(stateRegistry);
        StateApplier stateApplier = new StateApplier(stateRegistry, buffDebuffService);
        ResourceHandler resourceHandler = new ResourceHandler();
        CombatEngine combatEngine = new CombatEngine();

        CombatUnitSnapshot warrior = CombatUnitSnapshot.builder("warrior_01")
                .weaponPower(180)
                .equipmentPower(60)
                .generalDamageIncrease(0.20)
                .criticalChance(0.25)
                .criticalDamageMultiplier(1.70)
                .tagDamageIncrease("core", 0.10)
                .tagDamageIncrease("precision", 0.08)
                .initialResource("sword_intent", 4)
                .build();
        CombatUnitSnapshot assassin = CombatUnitSnapshot.builder("assassin_01")
                .weaponPower(170)
                .equipmentPower(45)
                .generalDamageIncrease(0.18)
                .criticalChance(0.30)
                .criticalDamageMultiplier(1.75)
                .tagDamageIncrease("precision", 0.12)
                .tagDamageIncrease("combo", 0.10)
                .initialResource("shadow_mark", 2)
                .build();
        CombatUnitSnapshot mage = CombatUnitSnapshot.builder("mage_01")
                .weaponPower(175)
                .equipmentPower(55)
                .generalDamageIncrease(0.15)
                .criticalChance(0.20)
                .criticalDamageMultiplier(1.65)
                .tagDamageIncrease("core", 0.10)
                .tagDamageIncrease("aoe", 0.09)
                .initialResource("resonance", 5)
                .build();
        CombatUnitSnapshot boss = CombatUnitSnapshot.builder("boss_ragnes")
                .weaponPower(0)
                .equipmentPower(0)
                .defense(180)
                .damageReduction(0.10)
                .build();

        Result<CombatLogEntry> case1 = combatEngine.execute(
                SkillExecutionContext.builder("gs_break_slash")
                        .attacker(warrior)
                        .defender(boss)
                        .skillRegistry(skillRegistry)
                        .stateRegistry(stateRegistry)
                        .buffDebuffService(buffDebuffService)
                        .stateApplier(stateApplier)
                        .resourceHandler(resourceHandler)
                        .criticalRoll(0.90)
                        .build()
        );
        assertTrue(case1.isOk());

        buffDebuffService.apply(boss.id(), stateRegistry.find("target_mark").orElseThrow(), 1);
        Result<CombatLogEntry> case2 = combatEngine.execute(
                SkillExecutionContext.builder("katana_draw_plus")
                        .attacker(warrior)
                        .defender(boss)
                        .skillRegistry(skillRegistry)
                        .stateRegistry(stateRegistry)
                        .buffDebuffService(buffDebuffService)
                        .stateApplier(stateApplier)
                        .resourceHandler(resourceHandler)
                        .immediateStateConsumeSkillIds(Set.of("katana_draw_plus"))
                        .criticalRoll(0.10)
                        .build()
        );
        assertTrue(case2.isOk());

        Result<CombatLogEntry> case3 = combatEngine.execute(
                SkillExecutionContext.builder("dagger_heart_cut")
                        .attacker(assassin)
                        .defender(boss)
                        .skillRegistry(skillRegistry)
                        .stateRegistry(stateRegistry)
                        .buffDebuffService(buffDebuffService)
                        .stateApplier(stateApplier)
                        .resourceHandler(resourceHandler)
                        .immediateStateConsumeSkillIds(Set.of("dagger_heart_cut"))
                        .criticalRoll(0.35)
                        .build()
        );
        assertTrue(case3.isOk());

        buffDebuffService.apply(boss.id(), stateRegistry.find("burn").orElseThrow(), 1);
        Result<CombatLogEntry> case4 = combatEngine.execute(
                SkillExecutionContext.builder("wand_dimension_break")
                        .attacker(mage)
                        .defender(boss)
                        .skillRegistry(skillRegistry)
                        .stateRegistry(stateRegistry)
                        .buffDebuffService(buffDebuffService)
                        .stateApplier(stateApplier)
                        .resourceHandler(resourceHandler)
                        .flag("rune_field_active", true)
                        .criticalRoll(0.05)
                        .build()
        );
        assertTrue(case4.isOk());

        System.out.println("case1_gs_break_slash=" + format(case1.value().finalDamage()));
        System.out.println("case2_katana_draw_plus=" + format(case2.value().finalDamage()));
        System.out.println("case3_dagger_heart_cut=" + format(case3.value().finalDamage()));
        System.out.println("case4_wand_dimension_break=" + format(case4.value().finalDamage()));
    }

    private void registerState(StateRegistry stateRegistry, String code, String group, boolean stackable, int maxStack) {
        stateRegistry.register(new StateDefinition(code, group, code, stackable, maxStack, 10.0d, ""));
    }

    private String format(double value) {
        return String.format("%.2f", value);
    }
}
