package io.fand.api.recipe;

import io.fand.api.item.ItemStack;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import net.kyori.adventure.key.Key;
import org.jspecify.annotations.Nullable;

/**
 * A shaped crafting recipe.
 */
public record ShapedRecipe(
        Key key,
        List<String> pattern,
        Map<Character, RecipeIngredient> ingredients,
        ItemStack result,
        @Nullable String groupName,
        CraftingRecipeCategory category,
        boolean showNotification
) implements Recipe {

    public ShapedRecipe(Key key, List<String> pattern, Map<Character, RecipeIngredient> ingredients, ItemStack result) {
        this(key, pattern, ingredients, result, null, CraftingRecipeCategory.MISC, true);
    }

    public ShapedRecipe(
            Key key,
            List<String> pattern,
            Map<Character, RecipeIngredient> ingredients,
            ItemStack result,
            @Nullable String groupName,
            CraftingRecipeCategory category,
            boolean showNotification
    ) {
        this.key = Objects.requireNonNull(key, "key");
        this.pattern = validatePattern(pattern);
        this.ingredients = validateIngredients(ingredients, this.pattern);
        this.result = validateResult(result);
        this.groupName = normalizeGroup(groupName);
        this.category = Objects.requireNonNull(category, "category");
        this.showNotification = showNotification;
    }

    @Override
    public RecipeType type() {
        return RecipeType.SHAPED;
    }

    @Override
    public Optional<String> group() {
        return Optional.ofNullable(groupName);
    }

    private static List<String> validatePattern(List<String> pattern) {
        Objects.requireNonNull(pattern, "pattern");
        if (pattern.isEmpty() || pattern.size() > 3) {
            throw new IllegalArgumentException("Shaped recipe pattern must have 1..3 rows");
        }
        int width = pattern.getFirst().length();
        if (width == 0 || width > 3) {
            throw new IllegalArgumentException("Shaped recipe pattern rows must have 1..3 columns");
        }
        boolean hasSymbol = false;
        for (var row : pattern) {
            Objects.requireNonNull(row, "pattern row");
            if (row.length() != width) {
                throw new IllegalArgumentException("Shaped recipe pattern rows must have equal width");
            }
            for (int index = 0; index < row.length(); index++) {
                if (row.charAt(index) != ' ') {
                    hasSymbol = true;
                }
            }
        }
        if (!hasSymbol) {
            throw new IllegalArgumentException("Shaped recipe pattern must contain at least one ingredient symbol");
        }
        return List.copyOf(pattern);
    }

    private static Map<Character, RecipeIngredient> validateIngredients(
            Map<Character, RecipeIngredient> ingredients,
            List<String> pattern
    ) {
        Objects.requireNonNull(ingredients, "ingredients");
        var copy = new LinkedHashMap<Character, RecipeIngredient>();
        for (var entry : ingredients.entrySet()) {
            var symbol = Objects.requireNonNull(entry.getKey(), "ingredient symbol");
            if (symbol == ' ') {
                throw new IllegalArgumentException("Space is reserved for empty shaped recipe slots");
            }
            copy.put(symbol, Objects.requireNonNull(entry.getValue(), "ingredient"));
        }

        var used = new java.util.HashSet<Character>();
        for (var row : pattern) {
            for (int index = 0; index < row.length(); index++) {
                char symbol = row.charAt(index);
                if (symbol == ' ') {
                    continue;
                }
                if (!copy.containsKey(symbol)) {
                    throw new IllegalArgumentException("Pattern references undefined ingredient symbol '" + symbol + "'");
                }
                used.add(symbol);
            }
        }
        for (char symbol : copy.keySet()) {
            if (!used.contains(symbol)) {
                throw new IllegalArgumentException("Ingredient symbol '" + symbol + "' is not used by the pattern");
            }
        }
        return Map.copyOf(copy);
    }

    static ItemStack validateResult(ItemStack result) {
        Objects.requireNonNull(result, "result");
        if (result.empty()) {
            throw new IllegalArgumentException("Recipe result must be non-empty");
        }
        return result;
    }

    static @Nullable String normalizeGroup(@Nullable String groupName) {
        return groupName == null || groupName.isBlank() ? null : groupName;
    }
}
