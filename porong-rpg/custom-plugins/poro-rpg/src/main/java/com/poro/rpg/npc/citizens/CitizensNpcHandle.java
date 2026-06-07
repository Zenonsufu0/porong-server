package com.poro.rpg.npc.citizens;

import org.bukkit.entity.EntityType;

public record CitizensNpcHandle(
        int npcId,
        String seedId,
        EntityType entityType,
        String displayName
) {
}

