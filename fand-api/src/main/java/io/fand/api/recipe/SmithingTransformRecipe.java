package io.fand.api.recipe;

import io.fand.api.item.ItemStack;
import java.util.Objects;
import java.util.Optional;
import net.kyori.adventure.key.Key;

/**
 * A smithing-table transform recipe, such as netherite equipment upgrades.
 */
public record SmithingTransformRecipe(
        Key key,
        Optional<RecipeIngredient> template,
        RecipeIngredient base,
        Optional<RecipeIngredient> addition,
        ItemStack result,
        boolean showNotification
) implements SmithingRecipe {

    public SmithingTransformRecipe(
            Key key,
            RecipeIngredient template,
            RecipeIngredient base,
            RecipeIngredient addition,
            ItemStack result
    ) {
        this(key, Optional.of(template), base, Optional.of(addition), result, true);
    }

    public SmithingTransformRecipe(
            Key key,
            Optional<RecipeIngredient> template,
            RecipeIngredient base,
            Optional<RecipeIngredient> addition,
            ItemStack result
    ) {
        this(key, template, base, addition, result, true);
    }

    public SmithingTransformRecipe {
        key = Objects.requireNonNull(key, "key");
        template = copyOptional(template, "template");
        base = Objects.requireNonNull(base, "base");
        addition = copyOptional(addition, "addition");
        result = ShapedRecipe.validateResult(result);
    }

    private static Optional<RecipeIngredient> copyOptional(Optional<RecipeIngredient> ingredient, String name) {
        Objects.requireNonNull(ingredient, name);
        return ingredient.map(value -> Objects.requireNonNull(value, name + " value"));
    }
}
