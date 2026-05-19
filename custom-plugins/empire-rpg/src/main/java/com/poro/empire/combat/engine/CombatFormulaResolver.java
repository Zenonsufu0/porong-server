package com.poro.empire.combat.engine;

import com.poro.empire.common.registry.master.model.SkillMaster;

public final class CombatFormulaResolver {
    public DamageResolution resolve(
            SkillMaster skill,
            SkillExecutionContext context,
            TagDamageResolver.ResolvedTagDamage tagDamage,
            ConditionalDamageResolver.ResolvedConditionalDamage conditionalDamage
    ) {
        double step1Coefficient = Math.max(0.0d, skill.baseCoefficient() / 100.0d);
        double step2BaseDamage = (context.attacker().weaponPower() + context.attacker().equipmentPower()) * step1Coefficient;
        double step3GeneralMultiplier = 1.0d + Math.max(0.0d, context.attacker().generalDamageIncrease());
        double step4TagMultiplier = Math.max(0.0d, tagDamage.multiplier());
        double step5ConditionalMultiplier = Math.max(0.0d, conditionalDamage.multiplier());
        double step6DefenseMultiplier = 200.0d / (200.0d + Math.max(0.0d, context.defender().defense()));

        double roll = context.criticalRoll().orElse(1.0d);
        boolean critical = roll < clamp(context.attacker().criticalChance(), 0.0d, 1.0d);
        double step7CriticalMultiplier = critical ? Math.max(1.0d, context.attacker().criticalDamageMultiplier()) : 1.0d;

        double targetReduction = clamp(
                context.defender().damageReduction() + context.buffDebuffService().additionalDamageReduction(context.defender().id()),
                0.0d,
                0.95d
        );
        double step8TargetReductionMultiplier = 1.0d - targetReduction;

        double step9FinalDamage = step2BaseDamage
                * step3GeneralMultiplier
                * step4TagMultiplier
                * step5ConditionalMultiplier
                * step6DefenseMultiplier
                * step7CriticalMultiplier
                * step8TargetReductionMultiplier;
        step9FinalDamage = Math.max(0.0d, step9FinalDamage);

        return new DamageResolution(
                step1Coefficient,
                step2BaseDamage,
                step3GeneralMultiplier,
                step4TagMultiplier,
                step5ConditionalMultiplier,
                step6DefenseMultiplier,
                step7CriticalMultiplier,
                step8TargetReductionMultiplier,
                step9FinalDamage,
                critical
        );
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    public record DamageResolution(
            double step1Coefficient,
            double step2BaseDamage,
            double step3GeneralMultiplier,
            double step4TagMultiplier,
            double step5ConditionalMultiplier,
            double step6DefenseMultiplier,
            double step7CriticalMultiplier,
            double step8TargetReductionMultiplier,
            double step9FinalDamage,
            boolean critical
    ) {
    }
}
