package com.poro.rpg.combat.engine;

import com.poro.rpg.common.registry.master.model.SkillMaster;
import com.poro.rpg.common.result.ErrorCode;
import com.poro.rpg.common.result.Result;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class CombatEngine {
    public Result<CombatLogEntry> execute(SkillExecutionContext context) {
        Result<SkillMaster> skillResult = resolveSkill(context);
        if (skillResult.isFailure()) {
            return Result.failure(skillResult.errorCode(), skillResult.message(), skillResult.cause());
        }
        SkillMaster skill = skillResult.value();

        CombatLogEntry.Builder logBuilder = new CombatLogEntry.Builder()
                .executedAt(Instant.now())
                .identifiers(skill.skillId(), context.attacker().id(), context.defender().id())
                .attackerResourcesBefore(context.attacker().resourceSnapshot());

        TagDamageResolver.ResolvedTagDamage tagDamage = context.tagDamageResolver().resolve(skill, context.attacker());
        ConditionalDamageResolver.ResolvedConditionalDamage conditionalDamage = context.conditionalDamageResolver().resolve(skill, context);
        CombatFormulaResolver.DamageResolution damage = context.combatFormulaResolver().resolve(skill, context, tagDamage, conditionalDamage);

        logBuilder.damageSteps(
                damage.step1Coefficient(),
                damage.step2BaseDamage(),
                damage.step3GeneralMultiplier(),
                damage.step4TagMultiplier(),
                damage.step5ConditionalMultiplier(),
                damage.step6DefenseMultiplier(),
                damage.step7CriticalMultiplier(),
                damage.step8TargetReductionMultiplier(),
                damage.step9FinalDamage(),
                damage.critical()
        );
        logBuilder.conditionalReason(conditionalDamage.reason());
        logBuilder.tagBreakdown(tagDamage.breakdown());
        logBuilder.addWarnings(conditionalDamage.warnings());

        // 상태 적용 순서:
        // 1) 적중(피해 확정)
        // 2) 표식/제어 부여
        // 3) 상태이상 부여
        // 4) 자원 획득/소모
        // 5) 후속 조건부 발동
        List<String> stateEvents = new ArrayList<>();
        String stateCode = normalized(skill.stateCode());
        if (!stateCode.isBlank()) {
            stateEvents.addAll(context.stateApplier().applyMarkOrControl(context.defender().id(), stateCode));
            stateEvents.addAll(context.stateApplier().applyStatus(context.defender().id(), stateCode));
        }

        ResourceHandler.ResourceMutationResult resourceResult = context.resourceHandler().apply(
                skill,
                context.attacker(),
                context.defender(),
                context.stateApplier(),
                context.buffDebuffService()
        );
        List<String> postHitEvents = context.conditionalDamageResolver().applyPostHitHooks(skill, context);

        logBuilder.addStateEvents(stateEvents);
        logBuilder.addStateEvents(postHitEvents);
        logBuilder.addResourceEvents(resourceResult.events());
        logBuilder.addWarnings(resourceResult.warnings());
        logBuilder.attackerResourcesAfter(context.attacker().resourceSnapshot());
        logBuilder.defenderStatesAfter(context.buffDebuffService().snapshot(context.defender().id()));

        return Result.success(logBuilder.build());
    }

    private Result<SkillMaster> resolveSkill(SkillExecutionContext context) {
        return context.skillRegistry().find(context.skillId())
                .map(Result::success)
                .orElseGet(() -> Result.failure(
                        ErrorCode.INVALID_ARGUMENT,
                        "Skill not found in registry: " + context.skillId()
                ));
    }

    private String normalized(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
