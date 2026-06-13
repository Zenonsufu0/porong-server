package kr.zenon.rpg.gui;

public final class ExploreHubGui {
    private ExploreHubGui() {
    }

    public interface FieldStateProvider {
        BossStatus status(String fieldId);
        int respawnMinutes(String fieldId);
        int playerCount(String fieldId);
    }

    public enum BossStatus {
        ALIVE,
        RESPAWNING,
        DEAD
    }
}
