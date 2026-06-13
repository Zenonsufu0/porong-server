package kr.zenon.rpg.reputation;

import kr.zenon.rpg.storage.PlayerData;
import kr.zenon.rpg.storage.PlayerDataManager;

import java.util.UUID;

public class ReputationManager {
    private final PlayerDataManager playerDataManager;

    public ReputationManager(PlayerDataManager playerDataManager) {
        this.playerDataManager = playerDataManager;
    }

    public int getReputation(UUID uuid) {
        return playerDataManager.getOrCreate(uuid).getReputation();
    }

    public void setReputation(UUID uuid, int amount) {
        PlayerData playerData = playerDataManager.getOrCreate(uuid);
        playerData.setReputation(amount);
    }

    public void addReputation(UUID uuid, int amount) {
        if (amount < 0) {
            removeReputation(uuid, Math.abs(amount));
            return;
        }
        int currentReputation = getReputation(uuid);
        setReputation(uuid, currentReputation + amount);
    }

    public void removeReputation(UUID uuid, int amount) {
        if (amount < 0) {
            addReputation(uuid, Math.abs(amount));
            return;
        }
        int currentReputation = getReputation(uuid);
        setReputation(uuid, currentReputation - amount);
    }

    public ReputationTier getTier(UUID uuid) {
        return ReputationTier.fromReputation(getReputation(uuid));
    }
}
