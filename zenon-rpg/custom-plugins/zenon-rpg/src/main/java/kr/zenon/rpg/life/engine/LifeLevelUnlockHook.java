package kr.zenon.rpg.life.engine;

public interface LifeLevelUnlockHook {
    void onLevelUnlocked(PlayerLifeState state, LifeType lifeType, int unlockedLevel, String unlockCode);
}
