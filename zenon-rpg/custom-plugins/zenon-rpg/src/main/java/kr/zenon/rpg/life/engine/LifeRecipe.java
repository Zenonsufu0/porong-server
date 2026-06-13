package kr.zenon.rpg.life.engine;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public record LifeRecipe(
        String recipeId,
        String recipeName,
        String recipeType,
        LifeType lifeType,
        int requiredLevel,
        String resultItemId,
        int resultAmount,
        int expGain,
        Map<String, Integer> materials
) {
    public LifeRecipe {
        recipeId = normalize(recipeId);
        recipeName = recipeName == null ? "" : recipeName.trim();
        recipeType = normalize(recipeType);
        resultItemId = normalize(resultItemId);
        requiredLevel = Math.max(1, requiredLevel);
        resultAmount = Math.max(1, resultAmount);
        expGain = Math.max(0, expGain);
        materials = normalizeMaterials(materials);
    }

    private static Map<String, Integer> normalizeMaterials(Map<String, Integer> raw) {
        Map<String, Integer> normalized = new LinkedHashMap<>();
        if (raw == null) {
            return Map.of();
        }
        for (Map.Entry<String, Integer> entry : raw.entrySet()) {
            String itemId = normalize(entry.getKey());
            int amount = Math.max(0, entry.getValue() == null ? 0 : entry.getValue());
            if (itemId.isBlank() || amount <= 0) {
                continue;
            }
            normalized.put(itemId, amount);
        }
        return Map.copyOf(normalized);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
