package com.poro.empire.growth.engine;

import java.util.List;
import java.util.Objects;

/**
 * 강화 로그 hook 합성 — onAttempt를 여러 hook(in-memory + DB)에 팬아웃.
 * (CompositeBossRunRecordHook 패턴.)
 */
public final class CompositeEnhancementLogHook implements EnhancementLogHook {
    private final List<EnhancementLogHook> delegates;

    public CompositeEnhancementLogHook(List<EnhancementLogHook> delegates) {
        this.delegates = List.copyOf(Objects.requireNonNull(delegates, "delegates"));
    }

    @Override
    public void onAttempt(EnhancementService.EnhancementResult result) {
        for (EnhancementLogHook hook : delegates) hook.onAttempt(result);
    }
}
