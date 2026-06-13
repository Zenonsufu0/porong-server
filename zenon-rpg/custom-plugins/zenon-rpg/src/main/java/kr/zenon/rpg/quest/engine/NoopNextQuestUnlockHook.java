package kr.zenon.rpg.quest.engine;

import java.util.List;

public final class NoopNextQuestUnlockHook implements NextQuestUnlockHook {
    @Override
    public void onNextQuestsUnlocked(PlayerQuestHonorState state, String completedQuestId, List<String> unlockedQuestIds) {
        // no-op
    }
}
