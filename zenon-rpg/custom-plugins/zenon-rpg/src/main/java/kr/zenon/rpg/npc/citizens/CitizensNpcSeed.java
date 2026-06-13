package kr.zenon.rpg.npc.citizens;

import org.bukkit.entity.EntityType;

import java.util.Locale;
import java.util.Objects;

public record CitizensNpcSeed(
        String npcSeedId,
        String npcMasterId,
        String regionCode,
        String townId,
        String worldName,
        double x,
        double y,
        double z,
        float yaw,
        float pitch,
        EntityType entityType,
        String displayName,
        String skinType,
        String skinValue,
        String roleType,
        String interactionProfileId,
        String questStartId,
        String betonConversationId,
        boolean protectedNpc,
        boolean lookClose,
        boolean shouldSpawn
) {
    public CitizensNpcSeed {
        Objects.requireNonNull(npcSeedId, "npcSeedId");
        Objects.requireNonNull(npcMasterId, "npcMasterId");
        Objects.requireNonNull(regionCode, "regionCode");
        Objects.requireNonNull(townId, "townId");
        Objects.requireNonNull(worldName, "worldName");
        Objects.requireNonNull(entityType, "entityType");
        Objects.requireNonNull(displayName, "displayName");
        Objects.requireNonNull(skinType, "skinType");
        Objects.requireNonNull(skinValue, "skinValue");
        Objects.requireNonNull(roleType, "roleType");
        Objects.requireNonNull(interactionProfileId, "interactionProfileId");
        Objects.requireNonNull(questStartId, "questStartId");
        Objects.requireNonNull(betonConversationId, "betonConversationId");
    }

    public String normalizedSkinType() {
        return skinType.trim().toLowerCase(Locale.ROOT);
    }

    public boolean hasInteractionProfile() {
        return !isBlankOrNone(interactionProfileId);
    }

    public boolean hasBetonConversation() {
        return !isBlankOrNone(betonConversationId);
    }

    public boolean hasQuestStartId() {
        return !isBlankOrNone(questStartId);
    }

    public boolean hasSkinValue() {
        return !isBlankOrNone(skinValue);
    }

    private static boolean isBlankOrNone(String value) {
        if (value == null || value.isBlank()) {
            return true;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        return "none".equals(normalized) || "-".equals(normalized);
    }
}

