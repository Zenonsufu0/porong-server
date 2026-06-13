package kr.zenon.rpg.growth.engine;

import kr.zenon.rpg.common.seed.CsvRow;

public final class GrowthSeedMappers {
    private GrowthSeedMappers() {
    }

    public static EnhancementRule enhancementRule(CsvRow row) {
        return new EnhancementRule(
                GrowthTier.from(row.required("tier")),
                row.required("enhance_level").isBlank() ? 0 : Integer.parseInt(row.required("enhance_level")),
                row.optionalDouble("success_rate", 0.0d),
                row.optionalInt("gold_cost", 0),
                row.optionalInt("stone_cost", 0),
                row.optionalBoolean("break_on_fail", false),
                row.optionalBoolean("downgrade_on_fail", false)
        );
    }

    public static PotentialOption potentialOption(CsvRow row) {
        return new PotentialOption(
                row.required("pool_id"),
                row.required("option_code"),
                PotentialGrade.from(row.required("grade")),
                row.optional("slot_type"),
                row.optional("class_filter"),
                row.optional("weapon_filter"),
                row.optional("tag_filter"),
                row.optionalDouble("value_min", 0.0d),
                row.optionalDouble("value_max", 0.0d),
                row.optionalInt("weight", 1)
        );
    }

    public static SetBonusRule setBonusRule(CsvRow row) {
        return new SetBonusRule(
                row.required("set_id"),
                row.required("set_name"),
                row.optionalInt("piece_count", 0),
                row.optionalInt("effect_order", 1),
                row.required("effect_type"),
                row.optional("value_type"),
                row.optionalDouble("value_amount", 0.0d),
                row.optional("notes")
        );
    }

    public static RuneMaster runeMaster(CsvRow row) {
        return new RuneMaster(
                row.required("rune_id"),
                row.required("rune_name"),
                row.required("rune_grade"),
                row.required("effect_type"),
                row.optional("value_type"),
                row.optionalDouble("value_amount", 0.0d),
                row.optional("slot_filter"),
                row.optional("notes")
        );
    }

    public static EngravingMaster engravingMaster(CsvRow row) {
        return new EngravingMaster(
                row.required("engraving_id"),
                row.required("engraving_name"),
                row.required("engraving_type"),
                row.optional("class_filter"),
                row.optionalInt("max_level", 1),
                row.required("effect_type"),
                row.optional("value_type"),
                row.optionalDouble("value_per_level", 0.0d),
                row.optional("notes")
        );
    }
}
