package com.poro.rpg.quest.engine;

import java.util.List;

public interface NextQuestUnlockHook {
    void onNextQuestsUnlocked(PlayerQuestHonorState state, String completedQuestId, List<String> unlockedQuestIds);
}
