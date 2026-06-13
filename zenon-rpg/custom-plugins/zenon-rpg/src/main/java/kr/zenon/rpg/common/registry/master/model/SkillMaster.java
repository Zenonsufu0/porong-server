package kr.zenon.rpg.common.registry.master.model;

public record SkillMaster(
        String skillId,
        String classId,
        String weaponId,
        String skillName,
        String roleType,
        String tag1,
        String tag2,
        double baseCoefficient,
        double cooldownSeconds,
        String resourceType,
        int resourceGain,
        int resourceCost,
        String secondaryEffectType,
        String secondaryEffectValue,
        String stateCode
) {
}
