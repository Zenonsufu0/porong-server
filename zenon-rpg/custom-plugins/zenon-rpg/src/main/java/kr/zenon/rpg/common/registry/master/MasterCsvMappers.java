package kr.zenon.rpg.common.registry.master;

import kr.zenon.rpg.common.registry.master.model.AchievementMaster;
import kr.zenon.rpg.common.registry.master.model.BossMaster;
import kr.zenon.rpg.common.registry.master.model.ItemMaster;
import kr.zenon.rpg.common.registry.master.model.NpcMaster;
import kr.zenon.rpg.common.registry.master.model.QuestMaster;
import kr.zenon.rpg.common.registry.master.model.RegionMaster;
import kr.zenon.rpg.common.registry.master.model.SkillMaster;
import kr.zenon.rpg.common.registry.master.model.TownMaster;
import kr.zenon.rpg.common.seed.CsvRow;

import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

final class MasterCsvMappers {
    private MasterCsvMappers() {
    }

    static ItemMaster item(CsvRow row) {
        return new ItemMaster(
                row.required("item_id"),
                row.required("item_name"),
                row.required("tier"),
                row.required("slot_type"),
                row.optional("region_theme"),
                row.optional("boss_source"),
                row.optional("set_id"),
                row.optional("base_stat_type"),
                row.optionalDouble("base_stat_value", 0.0d),
                row.optionalBoolean("is_tradeable", false),
                parseTags(row.optional("tags"))
        );
    }

    static SkillMaster skill(CsvRow row) {
        return new SkillMaster(
                row.required("skill_id"),
                row.optional("class_id"), // 1차 시즌은 클래스 미사용(무기 기반) — 빈 값 허용 (DL-098)
                row.required("weapon_id"),
                row.required("skill_name"),
                row.optional("role_type"),
                row.optional("tag_1"),
                row.optional("tag_2"),
                row.optionalDouble("base_coefficient", 0.0d),
                row.optionalDouble("cooldown_seconds", 0.0d),
                row.optional("resource_type"),
                row.optionalInt("resource_gain", 0),
                row.optionalInt("resource_cost", 0),
                row.optional("secondary_effect_type"),
                row.optional("secondary_effect_value"),
                row.optional("state_code")
        );
    }

    static BossMaster boss(CsvRow row) {
        return new BossMaster(
                row.required("boss_id"),
                row.required("boss_name"),
                row.required("boss_tier"),
                row.required("theme_type"),
                row.optionalBoolean("is_party_content", true),
                row.optionalBoolean("is_extreme", false)
        );
    }

    static QuestMaster quest(CsvRow row) {
        return new QuestMaster(
                row.required("quest_id"),
                row.required("quest_type"),
                row.required("quest_name"),
                row.optional("region_code"),
                row.optional("giver_npc_id"),
                row.optional("unlock_condition_type"),
                row.optional("unlock_condition_value"),
                row.optional("objective_type"),
                row.optional("objective_target_id"),
                row.optionalInt("objective_amount", 0),
                row.optional("reward_type"),
                row.optional("reward_target_id"),
                row.optionalInt("reward_amount", 0),
                row.optional("next_quest_id")
        );
    }

    static AchievementMaster achievement(CsvRow row) {
        return new AchievementMaster(
                row.required("achievement_id"),
                row.required("category"),
                row.required("achievement_name"),
                row.required("condition_type"),
                row.optional("condition_target_id"),
                row.optionalInt("condition_amount", 0),
                row.optional("reward_type"),
                row.optional("reward_target_id"),
                row.optionalInt("reward_amount", 0),
                row.optionalBoolean("is_hidden", false),
                row.optionalBoolean("is_repeatable", false),
                row.optional("notes")
        );
    }

    static RegionMaster region(CsvRow row) {
        return new RegionMaster(
                row.required("region_code"),
                row.required("region_name"),
                row.required("theme_type"),
                row.optionalBoolean("is_capital_related", false),
                row.optional("notes")
        );
    }

    static TownMaster town(CsvRow row) {
        return new TownMaster(
                row.required("town_id"),
                row.optional("region_code"),
                row.optional("town_grade"),
                row.required("town_name"),
                row.optionalBoolean("has_storage", false),
                row.optionalBoolean("has_blacksmith", false),
                row.optionalBoolean("has_life_support", false),
                row.optionalBoolean("has_boss_unlock_npc", false)
        );
    }

    static NpcMaster npc(CsvRow row) {
        return new NpcMaster(
                row.required("npc_id"),
                row.optional("region_code"),
                row.optional("town_id"),
                row.required("npc_name"),
                row.optional("npc_role_type"),
                row.optional("interaction_profile_id"),
                row.optionalBoolean("is_main_story_npc", false),
                row.optional("notes")
        );
    }

    private static Set<String> parseTags(String value) {
        if (value == null || value.isBlank()) {
            return Set.of();
        }
        return Arrays.stream(value.split("[|;]"))
                .map(token -> token.trim().toLowerCase(Locale.ROOT))
                .filter(token -> !token.isBlank() && !"-".equals(token))
                .collect(Collectors.toUnmodifiableSet());
    }
}
