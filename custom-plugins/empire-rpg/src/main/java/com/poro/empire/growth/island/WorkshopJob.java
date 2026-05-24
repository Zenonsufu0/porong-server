package com.poro.empire.growth.island;

public record WorkshopJob(
        String recipeId,
        long startedAt,
        long completeAt
) {
}
