package com.poro.rpg.common.registry.master.model;

public record BossMaster(
        String bossId,
        String bossName,
        String bossTier,
        String themeType,
        boolean partyContent,
        boolean extreme
) {
}
