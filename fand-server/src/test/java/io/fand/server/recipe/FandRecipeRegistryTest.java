package io.fand.server.recipe;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.fand.api.item.ItemStack;
import io.fand.api.item.ItemType;
import io.fand.api.recipe.ComplexRecipe;
import io.fand.api.recipe.CraftingRecipeCategory;
import io.fand.api.recipe.Recipe;
import io.fand.api.recipe.RecipeIngredient;
import io.fand.api.recipe.RecipeType;
import io.fand.api.recipe.ShapelessRecipe;
import io.fand.api.recipe.UnknownRecipe;
import java.util.List;
import net.kyori.adventure.key.Key;
import org.junit.jupiter.api.Test;

final class FandRecipeRegistryTest {

    private static final ItemType DIAMOND = new TestItemType(Key.key("minecraft:diamond"), 64);
    private static final RecipeIngredient STONE = RecipeIngredient.of(Key.key("minecraft:stone"));

    @Test
    void registersAndUnregistersCustomRecipesBeforeServerBinding() {
        var registry = new FandRecipeRegistry();
        var recipe = recipe(Key.key("fand:demo_recipe"), 1);

        var registration = registry.register(recipe);

        assertThat(registration.key()).isEqualTo(recipe.key());
        assertThat(registration.active()).isTrue();
        assertThat(registry.find(recipe.key()).map(Recipe::key)).contains(recipe.key());
        assertThat(registry.all().stream().map(Recipe::key)).containsExactly(recipe.key());
        assertThat(registry.byType(RecipeType.SHAPELESS).stream().map(Recipe::key)).containsExactly(recipe.key());

        registration.unregister();

        assertThat(registration.active()).isFalse();
        assertThat(registry.find(recipe.key())).isEmpty();
        assertThat(registry.all()).isEmpty();
    }

    @Test
    void oldRegistrationHandleCannotRemoveReplacement() {
        var registry = new FandRecipeRegistry();
        var key = Key.key("fand:replaceable");
        var first = recipe(key, 1);
        var second = recipe(key, 2);

        var firstRegistration = registry.register(first);
        var secondRegistration = registry.register(second);

        firstRegistration.unregister();

        assertThat(firstRegistration.active()).isFalse();
        assertThat(secondRegistration.active()).isTrue();
        assertThat(registry.find(key).map(Recipe::result)).contains(second.result());

        secondRegistration.unregister();

        assertThat(registry.find(key)).isEmpty();
    }

    @Test
    void removesCustomRecipesByKey() {
        var registry = new FandRecipeRegistry();
        var key = Key.key("fand:removable");
        registry.register(recipe(key, 1));

        assertThat(registry.remove(key)).isTrue();
        assertThat(registry.remove(key)).isFalse();
        assertThat(registry.find(key)).isEmpty();
    }

    @Test
    void rejectsReadOnlyAndUnknownRecipes() {
        var registry = new FandRecipeRegistry();

        assertThatThrownBy(() -> registry.register(new ComplexRecipe(
                Key.key("minecraft:firework_rocket"),
                Key.key("minecraft:firework_rocket"),
                ItemStack.EMPTY)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot be registered");
        assertThatThrownBy(() -> registry.register(new UnknownRecipe(Key.key("fand:unknown"), ItemStack.EMPTY, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot be registered");
    }

    private static ShapelessRecipe recipe(Key key, int amount) {
        return new ShapelessRecipe(
                key,
                List.of(STONE),
                new ItemStack(DIAMOND, amount),
                "test",
                CraftingRecipeCategory.MISC,
                true);
    }

    private record TestItemType(Key key, int maxStackSize) implements ItemType {
    }
}
