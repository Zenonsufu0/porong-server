package kr.zenon.rpg.reputation;

public enum ReputationTier {
    OUTSIDER(0, "이방인"),
    COMMONER(100, "평민"),
    RECOGNIZED(300, "인지됨"),
    HONORED(600, "명망가"),
    ELITE(1000, "엘리트");

    private final int minimumReputation;
    private final String displayName;

    ReputationTier(int minimumReputation, String displayName) {
        this.minimumReputation = minimumReputation;
        this.displayName = displayName;
    }

    public int getMinimumReputation() {
        return minimumReputation;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static ReputationTier fromReputation(int reputation) {
        if (reputation >= ELITE.minimumReputation) {
            return ELITE;
        }
        if (reputation >= HONORED.minimumReputation) {
            return HONORED;
        }
        if (reputation >= RECOGNIZED.minimumReputation) {
            return RECOGNIZED;
        }
        if (reputation >= COMMONER.minimumReputation) {
            return COMMONER;
        }
        return OUTSIDER;
    }
}
