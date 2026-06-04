package com.poro.rpg.boss.engine;

public interface UnlockQuestChecker {
    boolean hasUnlocked(String userId, String unlockQuestCode);
}
