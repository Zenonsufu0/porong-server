package kr.zenon.rpg.life.engine;

public final class AllowAllEstateUnlockQuestHook implements EstateUnlockQuestHook {
    @Override
    public boolean canUnlock(PlayerLifeState state, String unlockQuestId) {
        return true;
    }
}
