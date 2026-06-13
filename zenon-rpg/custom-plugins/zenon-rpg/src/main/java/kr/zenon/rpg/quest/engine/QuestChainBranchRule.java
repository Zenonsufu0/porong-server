package kr.zenon.rpg.quest.engine;

import java.util.Locale;

public record QuestChainBranchRule(
        String parentQuestId,
        String branchQuestId,
        String branchGroup,
        String notes
) {
    public QuestChainBranchRule {
        parentQuestId = normalize(parentQuestId);
        branchQuestId = normalize(branchQuestId);
        branchGroup = normalize(branchGroup);
        notes = notes == null ? "" : notes.trim();
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
