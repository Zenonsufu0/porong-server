package kr.zenon.rpg.life.engine;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

public record LifeCraftLogEntry(
        Instant craftedAt,
        String userId,
        String recipeId,
        int repeatCount,
        Map<String, Long> consumedMaterials,
        String resultItemId,
        long resultAmount
) {
    public LifeCraftLogEntry {
        consumedMaterials = consumedMaterials == null ? Map.of() : Map.copyOf(new LinkedHashMap<>(consumedMaterials));
        repeatCount = Math.max(1, repeatCount);
        resultAmount = Math.max(0L, resultAmount);
    }
}
