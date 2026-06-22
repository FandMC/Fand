package io.fand.api.recipe;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.fand.api.item.ItemStack;
import io.fand.api.item.ItemType;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.kyori.adventure.key.Key;
import org.junit.jupiter.api.Test;

final class RecipeModelsTest {

    private static final ItemType DIAMOND = new TestItemType(Key.key("minecraft:diamond"), 64);
    private static final ItemStack RESULT = new ItemStack(DIAMOND, 1);
    private static final RecipeIngredient STONE = RecipeIngredient.of(Key.key("minecraft:stone"));

    @Test
    void validatesShapedRecipes() {
        var recipe = new ShapedRecipe(
                Key.key("fand:compact_diamond"),
                List.of("AA", "AA"),
                Map.of('A', STONE),
                RESULT);

        assertThat(recipe.type()).isEqualTo(RecipeType.SHAPED);
        assertThat(recipe.pattern()).containsExactly("AA", "AA");
        assertThat(recipe.ingredients()).containsEntry('A', STONE);
        assertThat(recipe.group()).isEmpty();
    }

    @Test
    void rejectsInvalidShapedRecipes() {
        assertThatThrownBy(() -> new ShapedRecipe(Key.key("fand:empty"), List.of(), Map.of(), RESULT))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("1..3 rows");
        assertThatThrownBy(() -> new ShapedRecipe(Key.key("fand:wide"), List.of("AAAA"), Map.of('A', STONE), RESULT))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("1..3 columns");
        assertThatThrownBy(() -> new ShapedRecipe(Key.key("fand:ragged"), List.of("A", "AA"), Map.of('A', STONE), RESULT))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("equal width");
        assertThatThrownBy(() -> new ShapedRecipe(Key.key("fand:blank"), List.of(" "), Map.of(), RESULT))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("at least one ingredient symbol");
        assertThatThrownBy(() -> new ShapedRecipe(Key.key("fand:missing"), List.of("A"), Map.of(), RESULT))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("undefined ingredient symbol");
        assertThatThrownBy(() -> new ShapedRecipe(Key.key("fand:unused"), List.of("A"), Map.of('A', STONE, 'B', STONE), RESULT))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not used by the pattern");
        assertThatThrownBy(() -> new ShapedRecipe(Key.key("fand:empty_result"), List.of("A"), Map.of('A', STONE), ItemStack.EMPTY))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("non-empty");
    }

    @Test
    void validatesShapelessRecipes() {
        var recipe = new ShapelessRecipe(Key.key("fand:mixed"), List.of(STONE), RESULT, "group", CraftingRecipeCategory.MISC, false);

        assertThat(recipe.type()).isEqualTo(RecipeType.SHAPELESS);
        assertThat(recipe.ingredients()).containsExactly(STONE);
        assertThat(recipe.group()).contains("group");
        assertThat(recipe.showNotification()).isFalse();
    }

    @Test
    void rejectsInvalidShapelessRecipes() {
        assertThatThrownBy(() -> new ShapelessRecipe(Key.key("fand:none"), List.of(), RESULT))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("1..9 ingredients");
        assertThatThrownBy(() -> new ShapelessRecipe(Key.key("fand:too_many"), List.of(
                STONE, STONE, STONE, STONE, STONE, STONE, STONE, STONE, STONE, STONE), RESULT))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("1..9 ingredients");
    }

    @Test
    void validatesCookingRecipes() {
        var smelting = new CookingRecipe(Key.key("fand:smelt"), RecipeType.SMELTING, STONE, RESULT);
        var blasting = new CookingRecipe(Key.key("fand:blast"), RecipeType.BLASTING, STONE, RESULT);

        assertThat(smelting.type()).isEqualTo(RecipeType.SMELTING);
        assertThat(smelting.cookingTimeTicks()).isEqualTo(200);
        assertThat(blasting.cookingTimeTicks()).isEqualTo(100);
    }

    @Test
    void rejectsInvalidCookingRecipes() {
        assertThatThrownBy(() -> new CookingRecipe(Key.key("fand:crafting"), RecipeType.SHAPED, STONE, RESULT))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new CookingRecipe(
                Key.key("fand:negative_xp"),
                RecipeType.SMELTING,
                STONE,
                RESULT,
                -1.0F,
                200,
                null,
                CookingRecipeCategory.MISC,
                true))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("experience");
        assertThatThrownBy(() -> new CookingRecipe(
                Key.key("fand:no_time"),
                RecipeType.SMELTING,
                STONE,
                RESULT,
                0.0F,
                0,
                null,
                CookingRecipeCategory.MISC,
                true))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("positive");
    }

    @Test
    void validatesSmithingTransformRecipes() {
        var recipe = new SmithingTransformRecipe(
                Key.key("fand:upgrade"),
                RecipeIngredient.of("minecraft:netherite_upgrade_smithing_template"),
                RecipeIngredient.of("minecraft:diamond_sword"),
                RecipeIngredient.of("minecraft:netherite_ingot"),
                RESULT);

        assertThat(recipe.type()).isEqualTo(RecipeType.SMITHING);
        assertThat(recipe.type().smithing()).isTrue();
        assertThat(recipe.template()).contains(RecipeIngredient.of("minecraft:netherite_upgrade_smithing_template"));
        assertThat(recipe.base()).isEqualTo(RecipeIngredient.of("minecraft:diamond_sword"));
        assertThat(recipe.addition()).contains(RecipeIngredient.of("minecraft:netherite_ingot"));
        assertThat(recipe.result()).isEqualTo(RESULT);
    }

    @Test
    void smithingTransformSupportsOptionalTemplateAndAddition() {
        var recipe = new SmithingTransformRecipe(
                Key.key("fand:template_free"),
                Optional.empty(),
                STONE,
                Optional.empty(),
                RESULT,
                false);

        assertThat(recipe.template()).isEmpty();
        assertThat(recipe.addition()).isEmpty();
        assertThat(recipe.showNotification()).isFalse();
    }

    @Test
    void validatesSmithingTrimRecipes() {
        var recipe = new SmithingTrimRecipe(
                Key.key("fand:trim"),
                RecipeIngredient.of("minecraft:spire_armor_trim_smithing_template"),
                RecipeIngredient.of("minecraft:iron_chestplate"),
                RecipeIngredient.of("minecraft:amethyst_shard"),
                Key.key("minecraft:spire"),
                false);

        assertThat(recipe.type()).isEqualTo(RecipeType.SMITHING);
        assertThat(recipe.template()).contains(RecipeIngredient.of("minecraft:spire_armor_trim_smithing_template"));
        assertThat(recipe.addition()).contains(RecipeIngredient.of("minecraft:amethyst_shard"));
        assertThat(recipe.pattern()).isEqualTo(Key.key("minecraft:spire"));
        assertThat(recipe.result()).isEqualTo(ItemStack.EMPTY);
        assertThat(recipe.showNotification()).isFalse();
    }

    @Test
    void validatesComplexRecipes() {
        var recipe = new ComplexRecipe(
                Key.key("minecraft:firework_rocket"),
                Key.key("minecraft:firework_rocket"),
                ItemStack.EMPTY,
                "specials");

        assertThat(recipe.type()).isEqualTo(RecipeType.COMPLEX);
        assertThat(recipe.serializer()).isEqualTo(Key.key("minecraft:firework_rocket"));
        assertThat(recipe.group()).contains("specials");
    }

    @Test
    void validatesIngredients() {
        assertThat(RecipeIngredient.tag(Key.key("minecraft:planks")).isTag()).isTrue();
        assertThat(RecipeIngredient.ofKeys(List.of(Key.key("minecraft:stone"), Key.key("minecraft:cobblestone"))).items())
                .containsExactly(Key.key("minecraft:stone"), Key.key("minecraft:cobblestone"));
        assertThatThrownBy(() -> new RecipeIngredient(Key.key("minecraft:planks"), List.of(Key.key("minecraft:stone"))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("either a tag or items");
        assertThatThrownBy(() -> RecipeIngredient.ofKeys(List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("at least one item");
    }

    private record TestItemType(Key key, int maxStackSize) implements ItemType {
    }
}
