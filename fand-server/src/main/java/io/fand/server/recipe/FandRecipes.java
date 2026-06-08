package io.fand.server.recipe;

import io.fand.api.item.ItemStack;
import io.fand.api.recipe.CookingRecipeCategory;
import io.fand.api.recipe.CraftingRecipeCategory;
import io.fand.api.recipe.RecipeIngredient;
import io.fand.api.recipe.RecipeType;
import io.fand.api.recipe.UnknownRecipe;
import io.fand.server.item.FandItemStacks;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.kyori.adventure.key.Key;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.BlastingRecipe;
import net.minecraft.world.item.crafting.CampfireCookingRecipe;
import net.minecraft.world.item.crafting.CookingBookCategory;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.ShapedRecipePattern;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.item.crafting.SmokingRecipe;
import net.minecraft.world.item.crafting.StonecutterRecipe;

public final class FandRecipes {

    private FandRecipes() {
    }

    static RecipeHolder<?> toVanilla(io.fand.api.recipe.Recipe recipe, HolderLookup.Provider registries) {
        var key = recipeKey(recipe.key());
        return switch (recipe) {
            case io.fand.api.recipe.ShapedRecipe shaped -> new RecipeHolder<>(key, shaped(shaped, registries));
            case io.fand.api.recipe.ShapelessRecipe shapeless -> new RecipeHolder<>(key, shapeless(shapeless, registries));
            case io.fand.api.recipe.CookingRecipe cooking -> new RecipeHolder<>(key, cooking(cooking, registries));
            case io.fand.api.recipe.StonecuttingRecipe stonecutting -> new RecipeHolder<>(key, stonecutting(stonecutting, registries));
            default -> throw new IllegalArgumentException("Recipe type cannot be registered: " + recipe.type());
        };
    }

    public static io.fand.api.recipe.Recipe fromVanilla(RecipeHolder<?> holder) {
        var recipe = holder.value();
        var key = key(holder.id().identifier());
        return switch (recipe) {
            case net.minecraft.world.item.crafting.ShapedRecipe shaped -> shaped(key, shaped);
            case net.minecraft.world.item.crafting.ShapelessRecipe shapeless -> shapeless(key, shapeless);
            case AbstractCookingRecipe cooking -> cooking(key, cooking);
            case StonecutterRecipe stonecutting -> stonecutting(key, stonecutting);
            default -> new UnknownRecipe(key, assembledResult(recipe), recipe.group());
        };
    }

    static ResourceKey<net.minecraft.world.item.crafting.Recipe<?>> recipeKey(Key key) {
        return ResourceKey.create(Registries.RECIPE, id(key));
    }

    private static net.minecraft.world.item.crafting.ShapedRecipe shaped(
            io.fand.api.recipe.ShapedRecipe recipe,
            HolderLookup.Provider registries
    ) {
        var ingredients = new LinkedHashMap<Character, Ingredient>();
        recipe.ingredients().forEach((symbol, ingredient) -> ingredients.put(symbol, ingredient(ingredient, registries)));
        return new net.minecraft.world.item.crafting.ShapedRecipe(
                new net.minecraft.world.item.crafting.Recipe.CommonInfo(recipe.showNotification()),
                new net.minecraft.world.item.crafting.CraftingRecipe.CraftingBookInfo(
                        craftingCategory(recipe.category()),
                        recipe.group().orElse("")
                ),
                ShapedRecipePattern.of(ingredients, recipe.pattern()),
                result(recipe.result())
        );
    }

    private static net.minecraft.world.item.crafting.ShapelessRecipe shapeless(
            io.fand.api.recipe.ShapelessRecipe recipe,
            HolderLookup.Provider registries
    ) {
        return new net.minecraft.world.item.crafting.ShapelessRecipe(
                new net.minecraft.world.item.crafting.Recipe.CommonInfo(recipe.showNotification()),
                new net.minecraft.world.item.crafting.CraftingRecipe.CraftingBookInfo(
                        craftingCategory(recipe.category()),
                        recipe.group().orElse("")
                ),
                result(recipe.result()),
                recipe.ingredients().stream().map(ingredient -> ingredient(ingredient, registries)).toList()
        );
    }

    private static AbstractCookingRecipe cooking(
            io.fand.api.recipe.CookingRecipe recipe,
            HolderLookup.Provider registries
    ) {
        var commonInfo = new net.minecraft.world.item.crafting.Recipe.CommonInfo(recipe.showNotification());
        var bookInfo = new AbstractCookingRecipe.CookingBookInfo(
                cookingCategory(recipe.category()),
                recipe.group().orElse("")
        );
        var ingredient = ingredient(recipe.ingredient(), registries);
        var result = result(recipe.result());
        return switch (recipe.type()) {
            case SMELTING -> new SmeltingRecipe(
                    commonInfo,
                    bookInfo,
                    ingredient,
                    result,
                    recipe.experience(),
                    recipe.cookingTimeTicks()
            );
            case BLASTING -> new BlastingRecipe(
                    commonInfo,
                    bookInfo,
                    ingredient,
                    result,
                    recipe.experience(),
                    recipe.cookingTimeTicks()
            );
            case SMOKING -> new SmokingRecipe(
                    commonInfo,
                    bookInfo,
                    ingredient,
                    result,
                    recipe.experience(),
                    recipe.cookingTimeTicks()
            );
            case CAMPFIRE_COOKING -> new CampfireCookingRecipe(
                    commonInfo,
                    bookInfo,
                    ingredient,
                    result,
                    recipe.experience(),
                    recipe.cookingTimeTicks()
            );
            default -> throw new IllegalArgumentException("Not a cooking recipe type: " + recipe.type());
        };
    }

    private static StonecutterRecipe stonecutting(
            io.fand.api.recipe.StonecuttingRecipe recipe,
            HolderLookup.Provider registries
    ) {
        return new StonecutterRecipe(
                new net.minecraft.world.item.crafting.Recipe.CommonInfo(recipe.showNotification()),
                ingredient(recipe.ingredient(), registries),
                result(recipe.result())
        );
    }

    private static io.fand.api.recipe.ShapedRecipe shaped(Key key, net.minecraft.world.item.crafting.ShapedRecipe recipe) {
        var pattern = new ArrayList<String>();
        var ingredients = new LinkedHashMap<Character, RecipeIngredient>();
        var nextSymbol = 'A';
        var shapedIngredients = recipe.getIngredients();
        for (int row = 0; row < recipe.getHeight(); row++) {
            var builder = new StringBuilder();
            for (int column = 0; column < recipe.getWidth(); column++) {
                var ingredient = shapedIngredients.get(column + row * recipe.getWidth());
                if (ingredient.isEmpty()) {
                    builder.append(' ');
                } else {
                    var symbol = nextSymbol++;
                    builder.append(symbol);
                    ingredients.put(symbol, ingredient(ingredient.orElseThrow()));
                }
            }
            pattern.add(builder.toString());
        }
        return new io.fand.api.recipe.ShapedRecipe(
                key,
                pattern,
                ingredients,
                assembledResult(recipe),
                recipe.group(),
                craftingCategory(recipe.category()),
                recipe.showNotification()
        );
    }

    private static io.fand.api.recipe.ShapelessRecipe shapeless(Key key, net.minecraft.world.item.crafting.ShapelessRecipe recipe) {
        return new io.fand.api.recipe.ShapelessRecipe(
                key,
                recipe.placementInfo().ingredients().stream().map(FandRecipes::ingredient).toList(),
                assembledResult(recipe),
                recipe.group(),
                craftingCategory(recipe.category()),
                recipe.showNotification()
        );
    }

    private static io.fand.api.recipe.CookingRecipe cooking(Key key, AbstractCookingRecipe recipe) {
        return new io.fand.api.recipe.CookingRecipe(
                key,
                type(recipe.getType()),
                ingredient(recipe.input()),
                assembledResult(recipe),
                recipe.experience(),
                recipe.cookingTime(),
                recipe.group(),
                cookingCategory(recipe.category()),
                recipe.showNotification()
        );
    }

    private static io.fand.api.recipe.StonecuttingRecipe stonecutting(Key key, StonecutterRecipe recipe) {
        return new io.fand.api.recipe.StonecuttingRecipe(
                key,
                ingredient(recipe.input()),
                assembledResult(recipe),
                recipe.group(),
                recipe.showNotification()
        );
    }

    private static Ingredient ingredient(RecipeIngredient ingredient, HolderLookup.Provider registries) {
        if (ingredient.tag().isPresent()) {
            var tag = TagKey.create(Registries.ITEM, id(ingredient.tag().orElseThrow()));
            return Ingredient.of(registries.lookupOrThrow(Registries.ITEM).getOrThrow(tag));
        }
        return Ingredient.of(ingredient.items().stream().map(FandRecipes::item));
    }

    private static RecipeIngredient ingredient(Ingredient ingredient) {
        var tags = ingredient.items()
                .flatMap(Holder::tags)
                .map(tag -> key(tag.location()))
                .distinct()
                .toList();
        if (tags.size() == 1) {
            return RecipeIngredient.tag(tags.getFirst());
        }
        return RecipeIngredient.ofKeys(ingredient.items()
                .map(Holder::unwrapKey)
                .flatMap(Optional::stream)
                .map(ResourceKey::identifier)
                .map(FandRecipes::key)
                .distinct()
                .toList());
    }

    private static ItemStackTemplate result(ItemStack result) {
        return ItemStackTemplate.fromNonEmptyStack(FandItemStacks.toVanilla(result));
    }

    private static ItemStack assembledResult(net.minecraft.world.item.crafting.Recipe<?> recipe) {
        try {
            var input = switch (recipe.getType().toString()) {
                case "crafting" -> net.minecraft.world.item.crafting.CraftingInput.EMPTY;
                default -> new net.minecraft.world.item.crafting.SingleRecipeInput(net.minecraft.world.item.ItemStack.EMPTY);
            };
            return FandItemStacks.fromVanilla(((net.minecraft.world.item.crafting.Recipe) recipe).assemble(input));
        } catch (RuntimeException failure) {
            return ItemStack.EMPTY;
        }
    }

    private static Item item(Key key) {
        return BuiltInRegistries.ITEM.getOptional(id(key))
                .orElseThrow(() -> new IllegalArgumentException("Unknown item type: " + key.asString()));
    }

    private static RecipeType type(net.minecraft.world.item.crafting.RecipeType<?> type) {
        if (type == net.minecraft.world.item.crafting.RecipeType.SMELTING) {
            return RecipeType.SMELTING;
        }
        if (type == net.minecraft.world.item.crafting.RecipeType.BLASTING) {
            return RecipeType.BLASTING;
        }
        if (type == net.minecraft.world.item.crafting.RecipeType.SMOKING) {
            return RecipeType.SMOKING;
        }
        if (type == net.minecraft.world.item.crafting.RecipeType.CAMPFIRE_COOKING) {
            return RecipeType.CAMPFIRE_COOKING;
        }
        if (type == net.minecraft.world.item.crafting.RecipeType.STONECUTTING) {
            return RecipeType.STONECUTTING;
        }
        return RecipeType.UNKNOWN;
    }

    private static CraftingBookCategory craftingCategory(CraftingRecipeCategory category) {
        return switch (category) {
            case BUILDING -> CraftingBookCategory.BUILDING;
            case REDSTONE -> CraftingBookCategory.REDSTONE;
            case EQUIPMENT -> CraftingBookCategory.EQUIPMENT;
            case MISC -> CraftingBookCategory.MISC;
        };
    }

    private static CraftingRecipeCategory craftingCategory(CraftingBookCategory category) {
        return switch (category) {
            case BUILDING -> CraftingRecipeCategory.BUILDING;
            case REDSTONE -> CraftingRecipeCategory.REDSTONE;
            case EQUIPMENT -> CraftingRecipeCategory.EQUIPMENT;
            case MISC -> CraftingRecipeCategory.MISC;
        };
    }

    private static CookingBookCategory cookingCategory(CookingRecipeCategory category) {
        return switch (category) {
            case FOOD -> CookingBookCategory.FOOD;
            case BLOCKS -> CookingBookCategory.BLOCKS;
            case MISC -> CookingBookCategory.MISC;
        };
    }

    private static CookingRecipeCategory cookingCategory(CookingBookCategory category) {
        return switch (category) {
            case FOOD -> CookingRecipeCategory.FOOD;
            case BLOCKS -> CookingRecipeCategory.BLOCKS;
            case MISC -> CookingRecipeCategory.MISC;
        };
    }

    private static Identifier id(Key key) {
        return Identifier.fromNamespaceAndPath(key.namespace(), key.value());
    }

    public static Key key(Identifier id) {
        return Key.key(id.getNamespace(), id.getPath());
    }
}
