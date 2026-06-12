package com.poro.rpg.growth.engine;

public interface EnhancementLogHook {
    void onAttempt(EnhancementService.EnhancementResult result);
}
