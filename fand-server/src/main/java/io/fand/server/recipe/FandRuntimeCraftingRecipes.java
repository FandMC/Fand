package io.fand.server.recipe;

import io.fand.api.recipe.RecipeIngredient;
import io.fand.server.item.FandItemStacks;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.ShapedRecipePattern;
import net.minecraft.world.level.Level;

final class FandRuntimeCraftingRecipes {

    private FandRuntimeCraftingRecipes() {
    }

    static boolean hasRuntimeIngredient(io.fand.api.recipe.Recipe recipe) {
        return switch (recipe) {
            case io.fand.api.recipe.ShapedRecipe shaped -> shaped.ingredients().values().stream()
                    .anyMatch(RecipeIngredient::runtimeMatched);
            case io.fand.api.recipe.ShapelessRecipe shapeless -> shapeless.ingredients().stream()
                    .anyMatch(RecipeIngredient::runtimeMatched);
            default -> false;
        };
    }

    static CraftingRecipe shaped(
            io.fand.api.recipe.ShapedRecipe api,
            Map<Character, Ingredient> fallbackIngredients,
            ItemStackTemplate result,
            CraftingRecipe.CraftingBookInfo bookInfo
    ) {
        var patternRows = shrink(api.pattern());
        var pattern = ShapedRecipePattern.of(fallbackIngredients, patternRows);
        return new RuntimeShapedRecipe(
                shapedIngredients(api, fallbackIngredients, patternRows),
                new net.minecraft.world.item.crafting.Recipe.CommonInfo(api.showNotification()),
                bookInfo,
                pattern,
                result);
    }

    static CraftingRecipe shapeless(
            io.fand.api.recipe.ShapelessRecipe api,
            List<Ingredient> fallbackIngredients,
            ItemStackTemplate result,
            CraftingRecipe.CraftingBookInfo bookInfo
    ) {
        return new RuntimeShapelessRecipe(
                runtimeIngredients(api.ingredients(), fallbackIngredients),
                new net.minecraft.world.item.crafting.Recipe.CommonInfo(api.showNotification()),
                bookInfo,
                result,
                fallbackIngredients);
    }

    private static boolean matches(RecipeIngredient ingredient, net.minecraft.world.item.ItemStack stack) {
        return ingredient.matches(FandItemStacks.fromVanilla(stack));
    }

    private static final class RuntimeShapedRecipe extends net.minecraft.world.item.crafting.ShapedRecipe {

        private final int width;
        private final int height;
        private final List<Optional<RuntimeIngredient>> ingredients;
        private final boolean symmetrical;

        private RuntimeShapedRecipe(
                List<Optional<RuntimeIngredient>> ingredients,
                net.minecraft.world.item.crafting.Recipe.CommonInfo commonInfo,
                CraftingRecipe.CraftingBookInfo bookInfo,
                ShapedRecipePattern pattern,
                ItemStackTemplate result
        ) {
            super(commonInfo, bookInfo, pattern, result);
            this.width = pattern.width();
            this.height = pattern.height();
            this.ingredients = ingredients;
            this.symmetrical = symmetrical(this.width, this.height, this.ingredients);
        }

        @Override
        public boolean matches(CraftingInput input, Level level) {
            if (input.ingredientCount() != ingredients.stream().flatMap(Optional::stream).count()) {
                return false;
            }
            if (input.width() != width || input.height() != height) {
                return false;
            }
            return (!symmetrical && matches(input, true)) || matches(input, false);
        }

        private boolean matches(CraftingInput input, boolean xFlip) {
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    var expected = xFlip
                            ? ingredients.get(width - x - 1 + y * width)
                            : ingredients.get(x + y * width);
                    var actual = input.getItem(x, y);
                    if (expected.isEmpty()) {
                        if (!actual.isEmpty()) {
                            return false;
                        }
                    } else if (!expected.orElseThrow().matches(actual)) {
                        return false;
                    }
                }
            }
            return true;
        }
    }

    private static final class RuntimeShapelessRecipe extends net.minecraft.world.item.crafting.ShapelessRecipe {

        private final List<RuntimeIngredient> runtimeIngredients;

        private RuntimeShapelessRecipe(
                List<RuntimeIngredient> runtimeIngredients,
                net.minecraft.world.item.crafting.Recipe.CommonInfo commonInfo,
                CraftingRecipe.CraftingBookInfo bookInfo,
                ItemStackTemplate result,
                List<Ingredient> fallbackIngredients
        ) {
            super(commonInfo, bookInfo, result, fallbackIngredients);
            this.runtimeIngredients = runtimeIngredients;
        }

        @Override
        public boolean matches(CraftingInput input, Level level) {
            if (input.ingredientCount() != runtimeIngredients.size()) {
                return false;
            }
            var remaining = new ArrayList<>(runtimeIngredients);
            for (var stack : input.items()) {
                if (stack.isEmpty()) {
                    continue;
                }
                int index = firstMatching(remaining, stack);
                if (index < 0) {
                    return false;
                }
                remaining.remove(index);
            }
            return remaining.isEmpty();
        }

        private static int firstMatching(List<RuntimeIngredient> ingredients, net.minecraft.world.item.ItemStack stack) {
            for (int index = 0; index < ingredients.size(); index++) {
                if (ingredients.get(index).matches(stack)) {
                    return index;
                }
            }
            return -1;
        }
    }

    private static List<Optional<RuntimeIngredient>> shapedIngredients(
            io.fand.api.recipe.ShapedRecipe recipe,
            Map<Character, Ingredient> fallbackIngredientMap,
            List<String> pattern
    ) {
        var ingredients = new ArrayList<Optional<RuntimeIngredient>>();
        for (var row : pattern) {
            for (int index = 0; index < row.length(); index++) {
                char symbol = row.charAt(index);
                ingredients.add(symbol == ' '
                        ? Optional.empty()
                        : Optional.of(new RuntimeIngredient(recipe.ingredients().get(symbol), fallbackIngredientMap.get(symbol))));
            }
        }
        return List.copyOf(ingredients);
    }

    private static List<RuntimeIngredient> runtimeIngredients(List<RecipeIngredient> ingredients, List<Ingredient> fallbacks) {
        var runtime = new ArrayList<RuntimeIngredient>(ingredients.size());
        for (int index = 0; index < ingredients.size(); index++) {
            runtime.add(new RuntimeIngredient(ingredients.get(index), fallbacks.get(index)));
        }
        return List.copyOf(runtime);
    }

    private static boolean symmetrical(int width, int height, List<Optional<RuntimeIngredient>> ingredients) {
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width / 2; x++) {
                if (!ingredients.get(x + y * width).equals(ingredients.get(width - x - 1 + y * width))) {
                    return false;
                }
            }
        }
        return true;
    }

    private static List<String> shrink(List<String> pattern) {
        int left = Integer.MAX_VALUE;
        int right = 0;
        int top = 0;
        int bottom = 0;

        for (int rowIndex = 0; rowIndex < pattern.size(); rowIndex++) {
            String row = pattern.get(rowIndex);
            left = Math.min(left, firstNonEmpty(row));
            int lastNonEmpty = lastNonEmpty(row);
            right = Math.max(right, lastNonEmpty);
            if (lastNonEmpty < 0) {
                if (top == rowIndex) {
                    top++;
                }
                bottom++;
            } else {
                bottom = 0;
            }
        }

        var result = new ArrayList<String>();
        for (int row = top; row < pattern.size() - bottom; row++) {
            result.add(pattern.get(row).substring(left, right + 1));
        }
        return List.copyOf(result);
    }

    private static int firstNonEmpty(String row) {
        int index = 0;
        while (index < row.length() && row.charAt(index) == ' ') {
            index++;
        }
        return index;
    }

    private static int lastNonEmpty(String row) {
        int index = row.length() - 1;
        while (index >= 0 && row.charAt(index) == ' ') {
            index--;
        }
        return index;
    }

    private record RuntimeIngredient(RecipeIngredient api, Ingredient fallback) {

        boolean matches(net.minecraft.world.item.ItemStack stack) {
            return fallback.test(stack) && FandRuntimeCraftingRecipes.matches(api, stack);
        }
    }
}
