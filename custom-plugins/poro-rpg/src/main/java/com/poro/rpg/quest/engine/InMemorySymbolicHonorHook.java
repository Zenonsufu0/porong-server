package com.poro.rpg.quest.engine;

import com.poro.rpg.common.registry.master.model.AchievementMaster;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class InMemorySymbolicHonorHook implements SymbolicHonorHook {
    private final List<HonorGrant> grants = new ArrayList<>();

    @Override
    public void onSymbolicHonorGranted(PlayerQuestHonorState state, AchievementMaster achievement, String honorCode, long amount) {
        grants.add(new HonorGrant(state.userId(), achievement.achievementId(), honorCode, amount));
    }

    public List<HonorGrant> grants() {
        return Collections.unmodifiableList(grants);
    }

    public record HonorGrant(
            String userId,
            String achievementId,
            String honorCode,
            long amount
    ) {
    }
}
