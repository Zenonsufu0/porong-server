package com.poro.rpg.life.engine;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

public record EstateHarvestSummary(
        Instant harvestedAt,
        Map<String, Long> itemTotals,
        int facilityCount
) {
    public EstateHarvestSummary {
        itemTotals = itemTotals == null ? Map.of() : Map.copyOf(new LinkedHashMap<>(itemTotals));
        facilityCount = Math.max(0, facilityCount);
    }
}
