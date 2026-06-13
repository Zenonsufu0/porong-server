package kr.zenon.rpg.quest.engine;

import java.util.Locale;

public record QuestRewardEntry(
        String questId,
        int rewardOrder,
        QuestRewardType rewardType,
        String rewardTargetId,
        long rewardAmount,
        boolean firstClearOnly,
        String notes
) {
    public QuestRewardEntry {
        questId = normalize(questId);
        rewardOrder = Math.max(0, rewardOrder);
        rewardTargetId = normalize(rewardTargetId);
        rewardAmount = Math.max(0L, rewardAmount);
        notes = notes == null ? "" : notes.trim();
    }

    public String rewardKey() {
        return questId + "#" + rewardOrder;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
