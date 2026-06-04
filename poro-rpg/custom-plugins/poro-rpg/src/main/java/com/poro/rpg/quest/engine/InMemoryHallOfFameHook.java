package com.poro.rpg.quest.engine;

import com.poro.rpg.common.registry.master.model.AchievementMaster;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class InMemoryHallOfFameHook implements HallOfFameHook {
    private final List<HallOfFameEntry> entries = new ArrayList<>();

    @Override
    public void onAchievementRecorded(PlayerQuestHonorState state, AchievementMaster achievement, String recordId, long amount) {
        entries.add(new HallOfFameEntry(state.userId(), achievement.achievementId(), recordId, amount));
    }

    public List<HallOfFameEntry> entries() {
        return Collections.unmodifiableList(entries);
    }

    public record HallOfFameEntry(
            String userId,
            String achievementId,
            String recordId,
            long amount
    ) {
    }
}
