package io.fand.api.recipe;

import io.fand.api.item.ItemStack;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import net.kyori.adventure.key.Key;
import org.jspecify.annotations.Nullable;

/**
 * A shapeless crafting recipe.
 */
public record ShapelessRecipe(
        Key key,
        List<RecipeIngredient> ingredients,
        ItemStack result,
        @Nullable String groupName,
        CraftingRecipeCategory category,
        boolean showNotification
) implements Recipe {

    public ShapelessRecipe(Key key, List<RecipeIngredient> ingredients, ItemStack result) {
        this(key, ingredients, result, null, CraftingRecipeCategory.MISC, true);
    }

    public ShapelessRecipe(
            Key key,
            List<RecipeIngredient> ingredients,
            ItemStack result,
            @Nullable String groupName,
            CraftingRecipeCategory category,
            boolean showNotification
    ) {
        this.key = Objects.requireNonNull(key, "key");
        this.ingredients = validateIngredients(ingredients);
        this.result = ShapedRecipe.validateResult(result);
        this.groupName = ShapedRecipe.normalizeGroup(groupName);
        this.category = Objects.requireNonNull(category, "category");
        this.showNotification = showNotification;
    }

    @Override
    public RecipeType type() {
        return RecipeType.SHAPELESS;
    }

    @Override
    public Optional<String> group() {
        return Optional.ofNullable(groupName);
    }

    private static List<RecipeIngredient> validateIngredients(List<RecipeIngredient> ingredients) {
        Objects.requireNonNull(ingredients, "ingredients");
        if (ingredients.isEmpty() || ingredients.size() > 9) {
            throw new IllegalArgumentException("Shapeless recipe must have 1..9 ingredients");
        }
        return ingredients.stream()
                .map(ingredient -> Objects.requireNonNull(ingredient, "ingredient"))
                .toList();
    }
}
