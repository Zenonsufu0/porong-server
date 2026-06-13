package kr.zenon.rpg.life.engine;

import kr.zenon.rpg.common.seed.CsvRow;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public final class LifeSeedMappers {
    private LifeSeedMappers() {
    }

    public static LifeGatherNode gatherNode(CsvRow row) {
        return new LifeGatherNode(
                row.required("gather_id"),
                row.required("gather_name"),
                LifeType.from(row.required("life_type")),
                LifeSourceType.from(row.required("source_type")),
                row.required("base_item_id"),
                row.optionalInt("base_min", 1),
                row.optionalInt("base_max", 1),
                row.optional("rare_item_id"),
                row.optionalDouble("rare_chance_min", 0.0d),
                row.optionalDouble("rare_chance_max", 0.0d),
                row.optionalInt("exp_gain", 0),
                row.optionalInt("rare_exp_bonus_min", 0),
                row.optionalInt("rare_exp_bonus_max", 0),
                row.optional("theme_type")
        );
    }

    public static LifeRecipe recipe(CsvRow row) {
        return new LifeRecipe(
                row.required("recipe_id"),
                row.required("recipe_name"),
                row.required("recipe_type"),
                LifeType.from(row.required("life_type")),
                row.optionalInt("required_level", 1),
                row.required("result_item_id"),
                row.optionalInt("result_amount", 1),
                row.optionalInt("exp_gain", 0),
                parseMaterials(row.required("materials"))
        );
    }

    public static LifeSkillExpRule skillExpRule(CsvRow row) {
        return new LifeSkillExpRule(
                LifeType.from(row.required("life_type")),
                row.optionalInt("level", 1),
                row.optionalInt("cumulative_exp", 0),
                row.optionalDouble("gather_speed_bonus_pct", 0.0d),
                row.optionalDouble("yield_bonus_pct", 0.0d),
                row.optionalDouble("rare_bonus_pct", 0.0d),
                row.optionalDouble("estate_output_bonus_pct", 0.0d),
                row.optional("unlock_code")
        );
    }

    public static EstateUnlockRule estateUnlockRule(CsvRow row) {
        return new EstateUnlockRule(
                row.required("estate_id"),
                row.optional("unlock_quest_id"),
                row.optionalInt("initial_slot_capacity", 1),
                row.optional("first_install_facility_id")
        );
    }

    public static EstateFacilityMaster estateFacilityMaster(CsvRow row) {
        return new EstateFacilityMaster(
                row.required("facility_id"),
                row.required("facility_name"),
                row.optional("theme_type"),
                LifeType.from(row.required("life_type")),
                row.required("base_item_id"),
                row.optional("rare_item_id"),
                row.optionalInt("interval_minutes", 240),
                row.optionalInt("max_level", 3),
                row.optional("required_blueprint_id")
        );
    }

    public static EstateFacilityLevelRule estateFacilityLevelRule(CsvRow row) {
        return new EstateFacilityLevelRule(
                row.required("facility_id"),
                row.optionalInt("level", 1),
                row.optionalInt("base_min", 1),
                row.optionalInt("base_max", 1),
                row.optionalDouble("rare_chance_percent", 0.0d),
                row.optionalInt("storage_hours_cap", 24),
                row.optionalInt("upgrade_gold_cost", 0),
                row.optional("basic_cost_item_id"),
                row.optionalInt("basic_cost_amount", 0),
                row.optional("theme_cost_item_id"),
                row.optionalInt("theme_cost_amount", 0)
        );
    }

    private static Map<String, Integer> parseMaterials(String raw) {
        if (raw == null || raw.isBlank()) {
            return Map.of();
        }
        Map<String, Integer> materials = new LinkedHashMap<>();
        String[] tokens = raw.split("[|;]");
        for (String token : tokens) {
            if (token == null || token.isBlank()) {
                continue;
            }
            String[] pair = token.split(":");
            if (pair.length != 2) {
                continue;
            }
            String itemId = normalize(pair[0]);
            if (itemId.isBlank() || "-".equals(itemId)) {
                continue;
            }
            int amount;
            try {
                amount = Integer.parseInt(pair[1].trim());
            } catch (NumberFormatException exception) {
                amount = 0;
            }
            if (amount <= 0) {
                continue;
            }
            materials.put(itemId, amount);
        }
        return Map.copyOf(materials);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
