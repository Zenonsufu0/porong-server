package com.poro.rpg.common.registry.master.model;

public record NpcMaster(
        String npcId,
        String regionCode,
        String townId,
        String npcName,
        String npcRoleType,
        String interactionProfileId,
        boolean mainStoryNpc,
        String notes
) {
}
