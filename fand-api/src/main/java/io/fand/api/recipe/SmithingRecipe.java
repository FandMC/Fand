package io.fand.api.recipe;

import java.util.Optional;

/**
 * Base view of a smithing-table recipe.
 */
public interface SmithingRecipe extends Recipe {

    @Override
    default RecipeType type() {
        return RecipeType.SMITHING;
    }

    Optional<RecipeIngredient> template();

    RecipeIngredient base();

    Optional<RecipeIngredient> addition();

    @Override
    default Optional<String> group() {
        return Optional.empty();
    }
}
