package com.poro.rpg.life.engine;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public final class LifeRecipeRegistry {
    private final Map<String, LifeRecipe> values = new LinkedHashMap<>();

    public void register(LifeRecipe recipe) {
        values.put(normalize(recipe.recipeId()), recipe);
    }

    public Optional<LifeRecipe> find(String recipeId) {
        return Optional.ofNullable(values.get(normalize(recipeId)));
    }

    public List<LifeRecipe> listByLifeType(LifeType lifeType) {
        return values.values().stream()
                .filter(recipe -> recipe.lifeType() == lifeType)
                .collect(Collectors.toUnmodifiableList());
    }

    public Map<String, LifeRecipe> all() {
        return Map.copyOf(new LinkedHashMap<>(values));
    }

    public int size() {
        return values.size();
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
