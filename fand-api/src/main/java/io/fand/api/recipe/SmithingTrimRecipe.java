package io.fand.api.recipe;

import io.fand.api.item.ItemStack;
import java.util.Objects;
import java.util.Optional;
import net.kyori.adventure.key.Key;

/**
 * A smithing-table armor trim recipe.
 *
 * <p>The result is computed dynamically from the base item, trim material, and
 * trim pattern, so {@link #result()} returns {@link ItemStack#EMPTY}.
 */
public record SmithingTrimRecipe(
        Key key,
        RecipeIngredient templateIngredient,
        RecipeIngredient base,
        RecipeIngredient additionIngredient,
        Key pattern,
        boolean showNotification
) implements SmithingRecipe {

    public SmithingTrimRecipe(
            Key key,
            RecipeIngredient template,
            RecipeIngredient base,
            RecipeIngredient addition,
            Key pattern
    ) {
        this(key, template, base, addition, pattern, true);
    }

    public SmithingTrimRecipe {
        key = Objects.requireNonNull(key, "key");
        templateIngredient = Objects.requireNonNull(templateIngredient, "templateIngredient");
        base = Objects.requireNonNull(base, "base");
        additionIngredient = Objects.requireNonNull(additionIngredient, "additionIngredient");
        pattern = Objects.requireNonNull(pattern, "pattern");
    }

    @Override
    public Optional<RecipeIngredient> template() {
        return Optional.of(templateIngredient);
    }

    @Override
    public Optional<RecipeIngredient> addition() {
        return Optional.of(additionIngredient);
    }

    @Override
    public ItemStack result() {
        return ItemStack.EMPTY;
    }
}
