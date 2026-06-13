package kr.zenon.rpg.quest.engine;

import kr.zenon.rpg.common.seed.CsvRow;

public final class QuestSeedMappers {
    private QuestSeedMappers() {
    }

    public static QuestRewardEntry rewardEntry(CsvRow row) {
        return new QuestRewardEntry(
                row.required("quest_id"),
                row.optionalInt("reward_order", 1),
                QuestRewardType.from(row.required("reward_type")),
                row.optional("reward_target_id"),
                row.optionalInt("reward_amount", 0),
                row.optionalBoolean("is_first_clear_only", false),
                row.optional("notes")
        );
    }

    public static QuestChainBranchRule chainBranchRule(CsvRow row) {
        return new QuestChainBranchRule(
                row.required("parent_quest_id"),
                row.required("branch_quest_id"),
                row.optional("branch_group"),
                row.optional("notes")
        );
    }
}
