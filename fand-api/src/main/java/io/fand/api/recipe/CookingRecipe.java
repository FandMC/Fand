package io.fand.api.recipe;

import io.fand.api.item.ItemStack;
import java.util.Objects;
import java.util.Optional;
import net.kyori.adventure.key.Key;
import org.jspecify.annotations.Nullable;

/**
 * A furnace-like single-input recipe.
 */
public record CookingRecipe(
        Key key,
        RecipeType type,
        RecipeIngredient ingredient,
        ItemStack result,
        float experience,
        int cookingTimeTicks,
        @Nullable String groupName,
        CookingRecipeCategory category,
        boolean showNotification
) implements Recipe {

    public CookingRecipe(Key key, RecipeType type, RecipeIngredient ingredient, ItemStack result) {
        this(key, type, ingredient, result, 0.0F, defaultCookingTime(type), null, CookingRecipeCategory.MISC, true);
    }

    public CookingRecipe {
        key = Objects.requireNonNull(key, "key");
        type = Objects.requireNonNull(type, "type");
        if (!type.cooking()) {
            throw new IllegalArgumentException("CookingRecipe type must be a cooking type, got " + type);
        }
        ingredient = Objects.requireNonNull(ingredient, "ingredient");
        result = ShapedRecipe.validateResult(result);
        if (experience < 0.0F || !Float.isFinite(experience)) {
            throw new IllegalArgumentException("experience must be finite and >= 0");
        }
        if (cookingTimeTicks <= 0) {
            throw new IllegalArgumentException("cookingTimeTicks must be positive");
        }
        groupName = ShapedRecipe.normalizeGroup(groupName);
        category = Objects.requireNonNull(category, "category");
    }

    @Override
    public Optional<String> group() {
        return Optional.ofNullable(groupName);
    }

    public static int defaultCookingTime(RecipeType type) {
        return switch (Objects.requireNonNull(type, "type")) {
            case SMELTING -> 200;
            case BLASTING, SMOKING, CAMPFIRE_COOKING -> 100;
            default -> throw new IllegalArgumentException("Not a cooking recipe type: " + type);
        };
    }
}
