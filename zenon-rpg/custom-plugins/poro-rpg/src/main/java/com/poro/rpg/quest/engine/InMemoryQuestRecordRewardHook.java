package com.poro.rpg.quest.engine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class InMemoryQuestRecordRewardHook implements QuestRecordRewardHook {
    private final List<RecordGrant> grants = new ArrayList<>();

    @Override
    public void onQuestRecordReward(PlayerQuestHonorState state, String sourceId, String recordId, long amount) {
        grants.add(new RecordGrant(state.userId(), sourceId, recordId, amount));
    }

    public List<RecordGrant> grants() {
        return Collections.unmodifiableList(grants);
    }

    public record RecordGrant(
            String userId,
            String sourceId,
            String recordId,
            long amount
    ) {
    }
}
