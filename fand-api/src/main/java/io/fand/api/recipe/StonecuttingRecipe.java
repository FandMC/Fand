package io.fand.api.recipe;

import io.fand.api.item.ItemStack;
import java.util.Objects;
import java.util.Optional;
import net.kyori.adventure.key.Key;
import org.jspecify.annotations.Nullable;

/**
 * A stonecutter single-input recipe.
 */
public record StonecuttingRecipe(
        Key key,
        RecipeIngredient ingredient,
        ItemStack result,
        @Nullable String groupName,
        boolean showNotification
) implements Recipe {

    public StonecuttingRecipe(Key key, RecipeIngredient ingredient, ItemStack result) {
        this(key, ingredient, result, null, true);
    }

    public StonecuttingRecipe {
        key = Objects.requireNonNull(key, "key");
        ingredient = Objects.requireNonNull(ingredient, "ingredient");
        result = ShapedRecipe.validateResult(result);
        groupName = ShapedRecipe.normalizeGroup(groupName);
    }

    @Override
    public RecipeType type() {
        return RecipeType.STONECUTTING;
    }

    @Override
    public Optional<String> group() {
        return Optional.ofNullable(groupName);
    }
}
