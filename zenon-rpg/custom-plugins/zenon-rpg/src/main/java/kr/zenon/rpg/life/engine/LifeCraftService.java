package kr.zenon.rpg.life.engine;

import kr.zenon.rpg.common.result.ErrorCode;
import kr.zenon.rpg.common.result.Result;
import kr.zenon.rpg.common.time.TimeProvider;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class LifeCraftService {
    private final LifeRecipeRegistry recipeRegistry;
    private final LifeCraftLogHook craftLogHook;
    private final TimeProvider timeProvider;

    public LifeCraftService(
            LifeRecipeRegistry recipeRegistry,
            LifeCraftLogHook craftLogHook,
            TimeProvider timeProvider
    ) {
        this.recipeRegistry = Objects.requireNonNull(recipeRegistry, "recipeRegistry");
        this.craftLogHook = Objects.requireNonNull(craftLogHook, "craftLogHook");
        this.timeProvider = Objects.requireNonNull(timeProvider, "timeProvider");
    }

    public Optional<LifeRecipe> findRecipe(String recipeId) {
        return recipeRegistry.find(recipeId);
    }

    public List<LifeRecipe> recipesByLifeType(LifeType lifeType) {
        return recipeRegistry.listByLifeType(lifeType);
    }

    public Result<CraftResult> craft(PlayerLifeState state, String recipeId, int repeatCount) {
        if (state == null) {
            return Result.failure(ErrorCode.INVALID_ARGUMENT, "state is required.");
        }
        if (repeatCount <= 0) {
            return Result.failure(ErrorCode.INVALID_ARGUMENT, "repeatCount must be > 0.");
        }

        LifeRecipe recipe = recipeRegistry.find(recipeId).orElse(null);
        if (recipe == null) {
            return Result.failure(ErrorCode.INVALID_ARGUMENT, "Unknown recipe_id: " + recipeId);
        }

        PlayerLifeState.LifeSkillProfile profile = state.lifeProfile(recipe.lifeType());
        if (profile.level() < recipe.requiredLevel()) {
            return Result.failure(
                    ErrorCode.INVALID_ARGUMENT,
                    "Life level too low for recipe. recipe_id=" + recipe.recipeId()
                            + ", required=" + recipe.requiredLevel()
                            + ", current=" + profile.level()
            );
        }

        Map<String, Long> totalRequired = calculateTotalMaterials(recipe, repeatCount);
        for (Map.Entry<String, Long> requirement : totalRequired.entrySet()) {
            long currentAmount = state.itemAmount(requirement.getKey());
            if (currentAmount < requirement.getValue()) {
                return Result.failure(
                        ErrorCode.INVALID_ARGUMENT,
                        "Insufficient material. item_id=" + requirement.getKey()
                                + ", required=" + requirement.getValue()
                                + ", current=" + currentAmount
                );
            }
        }

        for (Map.Entry<String, Long> requirement : totalRequired.entrySet()) {
            state.consumeItem(requirement.getKey(), requirement.getValue());
        }

        long craftedAmount = (long) recipe.resultAmount() * repeatCount;
        state.addItem(recipe.resultItemId(), craftedAmount);

        Instant now = timeProvider.nowInstant();
        craftLogHook.onCrafted(new LifeCraftLogEntry(
                now,
                state.userId(),
                recipe.recipeId(),
                repeatCount,
                totalRequired,
                recipe.resultItemId(),
                craftedAmount
        ));

        long gainedExp = (long) recipe.expGain() * repeatCount;
        return Result.success(new CraftResult(
                recipe.recipeId(),
                recipe.recipeType(),
                repeatCount,
                totalRequired,
                recipe.resultItemId(),
                craftedAmount,
                gainedExp
        ));
    }

    private Map<String, Long> calculateTotalMaterials(LifeRecipe recipe, int repeatCount) {
        Map<String, Long> required = new LinkedHashMap<>();
        for (Map.Entry<String, Integer> material : recipe.materials().entrySet()) {
            String itemId = normalize(material.getKey());
            if (itemId.isBlank() || "-".equals(itemId)) {
                continue;
            }
            long amount = (long) Math.max(0, material.getValue()) * repeatCount;
            if (amount <= 0L) {
                continue;
            }
            required.put(itemId, amount);
        }
        return Map.copyOf(required);
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    public record CraftResult(
            String recipeId,
            String recipeType,
            int repeatCount,
            Map<String, Long> consumedMaterials,
            String resultItemId,
            long resultAmount,
            long gainedExp
    ) {
    }
}
