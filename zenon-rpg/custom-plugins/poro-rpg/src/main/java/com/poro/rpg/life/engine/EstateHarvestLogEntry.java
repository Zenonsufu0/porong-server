package com.poro.rpg.life.engine;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

public record EstateHarvestLogEntry(
        Instant harvestedAt,
        String userId,
        String estateId,
        int facilityCount,
        Map<String, Long> harvestedItems
) {
    public EstateHarvestLogEntry {
        facilityCount = Math.max(0, facilityCount);
        harvestedItems = harvestedItems == null ? Map.of() : Map.copyOf(new LinkedHashMap<>(harvestedItems));
    }
}
