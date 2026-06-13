package kr.zenon.rpg.growth.engine;

import java.util.ArrayList;
import java.util.List;

public final class InMemoryEnhancementLogHook implements EnhancementLogHook {
    private final List<EnhancementService.EnhancementResult> logs = new ArrayList<>();

    @Override
    public void onAttempt(EnhancementService.EnhancementResult result) {
        logs.add(result);
    }

    public List<EnhancementService.EnhancementResult> logs() {
        return List.copyOf(logs);
    }
}
