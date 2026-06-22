package io.fand.server.recipe;

import io.fand.api.item.ItemStack;
import io.fand.api.recipe.CookingRecipeCategory;
import io.fand.api.recipe.CraftingRecipeCategory;
import io.fand.api.recipe.ComplexRecipe;
import io.fand.api.recipe.RecipeIngredient;
import io.fand.api.recipe.RecipeType;
import io.fand.api.recipe.SmithingTransformRecipe;
import io.fand.api.recipe.SmithingTrimRecipe;
import io.fand.api.recipe.UnknownRecipe;
import io.fand.server.item.FandItemStacks;
import io.fand.server.util.ReflectionFields;
import java.lang.reflect.Field;
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
import net.minecraft.world.item.crafting.CustomRecipe;
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
import net.minecraft.world.item.equipment.trim.TrimPattern;

public final class FandRecipes {

    private static final Field SMITHING_TRANSFORM_RESULT = ReflectionFields.field(
            net.minecraft.world.item.crafting.SmithingTransformRecipe.class, "result");
    private static final Field SMITHING_TRIM_PATTERN = ReflectionFields.field(
            net.minecraft.world.item.crafting.SmithingTrimRecipe.class, "pattern");

    private FandRecipes() {
    }

    static RecipeHolder<?> toVanilla(io.fand.api.recipe.Recipe recipe, HolderLookup.Provider registries) {
        var key = recipeKey(recipe.key());
        return switch (recipe) {
            case io.fand.api.recipe.ShapedRecipe shaped -> new RecipeHolder<>(key, shaped(shaped, registries));
            case io.fand.api.recipe.ShapelessRecipe shapeless -> new RecipeHolder<>(key, shapeless(shapeless, registries));
            case io.fand.api.recipe.CookingRecipe cooking -> new RecipeHolder<>(key, cooking(cooking, registries));
            case io.fand.api.recipe.StonecuttingRecipe stonecutting -> new RecipeHolder<>(key, stonecutting(stonecutting, registries));
            case SmithingTransformRecipe smithing -> new RecipeHolder<>(key, smithingTransform(smithing, registries));
            case SmithingTrimRecipe smithing -> new RecipeHolder<>(key, smithingTrim(smithing, registries));
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
            case net.minecraft.world.item.crafting.SmithingTransformRecipe smithing -> smithingTransform(key, smithing);
            case net.minecraft.world.item.crafting.SmithingTrimRecipe smithing -> smithingTrim(key, smithing);
            case CustomRecipe custom -> complex(key, custom);
            default -> new UnknownRecipe(key, assembledResult(recipe), recipe.group());
        };
    }

    public static ResourceKey<net.minecraft.world.item.crafting.Recipe<?>> recipeKey(Key key) {
        return ResourceKey.create(Registries.RECIPE, id(key));
    }

    private static net.minecraft.world.item.crafting.CraftingRecipe shaped(
            io.fand.api.recipe.ShapedRecipe recipe,
            HolderLookup.Provider registries
    ) {
        var ingredients = new LinkedHashMap<Character, Ingredient>();
        recipe.ingredients().forEach((symbol, ingredient) -> ingredients.put(symbol, ingredient(ingredient, registries)));
        var bookInfo = new net.minecraft.world.item.crafting.CraftingRecipe.CraftingBookInfo(
                craftingCategory(recipe.category()),
                recipe.group().orElse("")
        );
        if (FandRuntimeCraftingRecipes.hasRuntimeIngredient(recipe)) {
            return FandRuntimeCraftingRecipes.shaped(
                    recipe,
                    ingredients,
                    result(recipe.result()),
                    bookInfo);
        }
        return new net.minecraft.world.item.crafting.ShapedRecipe(
                new net.minecraft.world.item.crafting.Recipe.CommonInfo(recipe.showNotification()),
                bookInfo,
                ShapedRecipePattern.of(ingredients, recipe.pattern()),
                result(recipe.result())
        );
    }

    private static net.minecraft.world.item.crafting.SmithingTransformRecipe smithingTransform(
            SmithingTransformRecipe recipe,
            HolderLookup.Provider registries
    ) {
        return new net.minecraft.world.item.crafting.SmithingTransformRecipe(
                new net.minecraft.world.item.crafting.Recipe.CommonInfo(recipe.showNotification()),
                recipe.template().map(value -> ingredient(value, registries)),
                ingredient(recipe.base(), registries),
                recipe.addition().map(value -> ingredient(value, registries)),
                result(recipe.result())
        );
    }

    private static net.minecraft.world.item.crafting.SmithingTrimRecipe smithingTrim(
            SmithingTrimRecipe recipe,
            HolderLookup.Provider registries
    ) {
        var pattern = registries.lookupOrThrow(Registries.TRIM_PATTERN)
                .getOrThrow(ResourceKey.create(Registries.TRIM_PATTERN, id(recipe.pattern())));
        return new net.minecraft.world.item.crafting.SmithingTrimRecipe(
                new net.minecraft.world.item.crafting.Recipe.CommonInfo(recipe.showNotification()),
                ingredient(recipe.templateIngredient(), registries),
                ingredient(recipe.base(), registries),
                ingredient(recipe.additionIngredient(), registries),
                pattern
        );
    }

    private static net.minecraft.world.item.crafting.CraftingRecipe shapeless(
            io.fand.api.recipe.ShapelessRecipe recipe,
            HolderLookup.Provider registries
    ) {
        var ingredients = recipe.ingredients().stream().map(ingredient -> ingredient(ingredient, registries)).toList();
        var bookInfo = new net.minecraft.world.item.crafting.CraftingRecipe.CraftingBookInfo(
                craftingCategory(recipe.category()),
                recipe.group().orElse("")
        );
        if (FandRuntimeCraftingRecipes.hasRuntimeIngredient(recipe)) {
            return FandRuntimeCraftingRecipes.shapeless(
                    recipe,
                    ingredients,
                    result(recipe.result()),
                    bookInfo);
        }
        return new net.minecraft.world.item.crafting.ShapelessRecipe(
                new net.minecraft.world.item.crafting.Recipe.CommonInfo(recipe.showNotification()),
                bookInfo,
                result(recipe.result()),
                ingredients
        );
    }

    private static SmithingTransformRecipe smithingTransform(
            Key key,
            net.minecraft.world.item.crafting.SmithingTransformRecipe recipe
    ) {
        var result = ((ItemStackTemplate) ReflectionFields.value(SMITHING_TRANSFORM_RESULT, recipe)).create();
        return new SmithingTransformRecipe(
                key,
                recipe.templateIngredient().map(FandRecipes::ingredient),
                ingredient(recipe.baseIngredient()),
                recipe.additionIngredient().map(FandRecipes::ingredient),
                FandItemStacks.fromVanilla(result),
                recipe.showNotification()
        );
    }

    @SuppressWarnings("unchecked")
    private static SmithingTrimRecipe smithingTrim(Key key, net.minecraft.world.item.crafting.SmithingTrimRecipe recipe) {
        var pattern = (Holder<TrimPattern>) ReflectionFields.value(SMITHING_TRIM_PATTERN, recipe);
        var patternKey = pattern.unwrapKey()
                .map(ResourceKey::identifier)
                .map(FandRecipes::key)
                .orElseGet(() -> key(pattern.value().assetId()));
        return new SmithingTrimRecipe(
                key,
                ingredient(recipe.templateIngredient().orElseThrow()),
                ingredient(recipe.baseIngredient()),
                ingredient(recipe.additionIngredient().orElseThrow()),
                patternKey,
                recipe.showNotification()
        );
    }

    private static ComplexRecipe complex(Key key, CustomRecipe recipe) {
        return new ComplexRecipe(key, serializerKey(recipe), assembledResult(recipe), recipe.group());
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

    private static Key serializerKey(net.minecraft.world.item.crafting.Recipe<?> recipe) {
        var id = BuiltInRegistries.RECIPE_SERIALIZER.getKey(recipe.getSerializer());
        return id == null ? Key.key("minecraft:unknown") : key(id);
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
