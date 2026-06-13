package com.poro.rpg.common.registry.master;

import com.poro.rpg.common.logging.DomainLogger;
import com.poro.rpg.common.registry.master.model.ItemMaster;
import com.poro.rpg.common.registry.master.model.NpcMaster;
import com.poro.rpg.common.registry.master.model.QuestMaster;
import com.poro.rpg.common.registry.master.model.SkillMaster;
import com.poro.rpg.common.registry.master.model.TownMaster;
import com.poro.rpg.common.result.ErrorCode;
import com.poro.rpg.common.result.Result;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

final class MasterSeedValidator {
    private static final Set<String> VALID_TAG_CODES = Set.of(
            "core", "precision", "aoe", "combo", "builder", "spender", "finisher", "support", "mobility", "starter", "boss_drop"
    );

    private static final Set<String> VALID_STATE_CODES = Set.of(
            "bleed", "poison", "burn", "frost_erosion",
            "slow", "bind", "stagger", "freeze",
            "target_mark", "crack", "exposed_weakness", "stigma", "shadow_mark",
            "recovery_boost", "damage_reduction", "movement_boost", "shield_active"
    );

    private MasterSeedValidator() {
    }

    static Result<Void> validate(MasterRegistryContext context, DomainLogger logger) {
        List<String> blockingErrors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        validateItems(context, blockingErrors, warnings);
        validateTowns(context, blockingErrors, warnings);
        validateNpcs(context, blockingErrors, warnings);
        validateQuestLinks(context, warnings);
        validateTagAndStateCodes(context, warnings);

        for (String warning : warnings) {
            logger.warn(warning);
        }

        if (!blockingErrors.isEmpty()) {
            blockingErrors.forEach(logger::error);
            String summary = "Master seed validation failed with " + blockingErrors.size() + " blocking errors.";
            return Result.failure(ErrorCode.MASTER_SEED_VALIDATION_FAILED, summary);
        }

        logger.info("Master seed validation passed. warnings=" + warnings.size());
        return Result.success();
    }

    private static void validateItems(MasterRegistryContext context, List<String> blockingErrors, List<String> warnings) {
        for (ItemMaster item : context.itemMasters().all().values()) {
            if ("t2".equalsIgnoreCase(item.tier()) && isBlank(item.bossSource())) {
                blockingErrors.add("Blocked: T2 item '" + item.itemId() + "' has no boss_source.");
            }

            if (!isBlank(item.bossSource()) && !context.bossMasters().contains(item.bossSource())) {
                warnings.add("Warning: item '" + item.itemId() + "' references missing boss_source '" + item.bossSource() + "'.");
            }
        }
    }

    private static void validateTowns(MasterRegistryContext context, List<String> blockingErrors, List<String> warnings) {
        for (TownMaster town : context.townMasters().all().values()) {
            if (isBlank(town.regionCode())) {
                blockingErrors.add("Blocked: town '" + town.townId() + "' has no region_code.");
                continue;
            }

            if (!context.regionMasters().contains(town.regionCode())) {
                warnings.add("Warning: town '" + town.townId() + "' references missing region_code '" + town.regionCode() + "'.");
            }
        }
    }

    private static void validateNpcs(MasterRegistryContext context, List<String> blockingErrors, List<String> warnings) {
        for (NpcMaster npc : context.npcMasters().all().values()) {
            if (isBlank(npc.townId())) {
                blockingErrors.add("Blocked: npc '" + npc.npcId() + "' has no town_id.");
                continue;
            }

            if (!context.townMasters().contains(npc.townId())) {
                warnings.add("Warning: npc '" + npc.npcId() + "' references missing town_id '" + npc.townId() + "'.");
            }
        }
    }

    private static void validateQuestLinks(MasterRegistryContext context, List<String> warnings) {
        for (QuestMaster quest : context.questMasters().all().values()) {
            String nextQuestId = normalized(quest.nextQuestId());
            if (nextQuestId.isBlank() || "none".equals(nextQuestId)) {
                continue;
            }

            if (!context.questMasters().contains(nextQuestId)) {
                warnings.add("Warning: quest '" + quest.questId() + "' references missing next_quest_id '" + nextQuestId + "'.");
            }
        }
    }

    private static void validateTagAndStateCodes(MasterRegistryContext context, List<String> warnings) {
        for (ItemMaster item : context.itemMasters().all().values()) {
            for (String tag : item.tags()) {
                if (!VALID_TAG_CODES.contains(normalized(tag))) {
                    warnings.add("Warning: item '" + item.itemId() + "' uses unknown tag code '" + tag + "'.");
                }
            }
        }

        for (SkillMaster skill : context.skillMasters().all().values()) {
            checkTag("skill", skill.skillId(), skill.tag1(), warnings);
            checkTag("skill", skill.skillId(), skill.tag2(), warnings);

            String stateCode = normalized(skill.stateCode());
            if (!stateCode.isBlank() && !VALID_STATE_CODES.contains(stateCode)) {
                warnings.add("Warning: skill '" + skill.skillId() + "' uses unknown state code '" + skill.stateCode() + "'.");
            }
        }
    }

    private static void checkTag(String entityType, String entityId, String tagCode, List<String> warnings) {
        String normalizedTag = normalized(tagCode);
        if (normalizedTag.isBlank() || "-".equals(normalizedTag)) {
            return;
        }
        if (!VALID_TAG_CODES.contains(normalizedTag)) {
            warnings.add("Warning: " + entityType + " '" + entityId + "' uses unknown tag code '" + tagCode + "'.");
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static String normalized(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
