package kr.zenon.rpg.life.engine;

public interface EstateUnlockQuestHook {
    boolean canUnlock(PlayerLifeState state, String unlockQuestId);
}
