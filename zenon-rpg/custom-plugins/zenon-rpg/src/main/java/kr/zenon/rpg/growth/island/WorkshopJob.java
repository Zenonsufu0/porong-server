package kr.zenon.rpg.growth.island;

public record WorkshopJob(
        String recipeId,
        long startedAt,
        long completeAt
) {
}
