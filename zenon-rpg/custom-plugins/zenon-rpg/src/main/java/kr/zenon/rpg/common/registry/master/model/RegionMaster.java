package kr.zenon.rpg.common.registry.master.model;

public record RegionMaster(
        String regionCode,
        String regionName,
        String themeType,
        boolean capitalRelated,
        String notes
) {
}
