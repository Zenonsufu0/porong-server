package com.poro.rpg.common.registry.master.model;

import java.util.Set;

public record ItemMaster(
        String itemId,
        String itemName,
        String tier,
        String slotType,
        String regionTheme,
        String bossSource,
        String setId,
        String baseStatType,
        double baseStatValue,
        boolean tradeable,
        Set<String> tags
) {
}
