package kr.zenon.rpg.life.engine;

public final class NoopLifeLevelUnlockHook implements LifeLevelUnlockHook {
    @Override
    public void onLevelUnlocked(PlayerLifeState state, LifeType lifeType, int unlockedLevel, String unlockCode) {
        // no-op
    }
}
