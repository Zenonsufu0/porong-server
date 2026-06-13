package com.poro.rpg.boss.engine;

public final class AllowAllUnlockQuestChecker implements UnlockQuestChecker {
    @Override
    public boolean hasUnlocked(String userId, String unlockQuestCode) {
        return true;
    }
}
