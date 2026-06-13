package kr.zenon.rpg.common.registry.master.model;

public record TownMaster(
        String townId,
        String regionCode,
        String townGrade,
        String townName,
        boolean hasStorage,
        boolean hasBlacksmith,
        boolean hasLifeSupport,
        boolean hasBossUnlockNpc
) {
}
