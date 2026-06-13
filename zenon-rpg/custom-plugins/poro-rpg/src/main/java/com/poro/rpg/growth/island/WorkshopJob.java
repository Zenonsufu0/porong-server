package com.poro.rpg.growth.island;

public record WorkshopJob(
        String recipeId,
        long startedAt,
        long completeAt
) {
}
