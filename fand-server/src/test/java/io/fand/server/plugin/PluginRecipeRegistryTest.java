package io.fand.server.plugin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.fand.api.item.ItemStack;
import io.fand.api.item.ItemType;
import io.fand.api.recipe.Recipe;
import io.fand.api.recipe.RecipeIngredient;
import io.fand.api.recipe.RecipeType;
import io.fand.api.recipe.ShapelessRecipe;
import io.fand.api.recipe.UnknownRecipe;
import io.fand.server.recipe.FandRecipeRegistry;
import java.util.List;
import net.kyori.adventure.key.Key;
import org.junit.jupiter.api.Test;

final class PluginRecipeRegistryTest {

    private static final ItemType DIAMOND = new TestItemType(Key.key("minecraft:diamond"), 64);
    private static final RecipeIngredient STONE = RecipeIngredient.of(Key.key("minecraft:stone"));

    @Test
    void scopesRegisteredRecipesToPluginNamespace() {
        var delegate = new FandRecipeRegistry();
        var tracker = new PluginResourceTracker();
        var registry = new PluginRecipeRegistry(delegate, tracker, "demo");
        var recipe = recipe(Key.key("external:custom_diamond"), 1);

        var registration = registry.register(recipe);

        assertThat(registration.key()).isEqualTo(Key.key("demo:custom_diamond"));
        assertThat(delegate.find(Key.key("external:custom_diamond"))).isEmpty();
        assertThat(delegate.find(Key.key("demo:custom_diamond"))).isPresent();
        assertThat(registry.find(Key.key("other:custom_diamond"))).isPresent();
        assertThat(registry.byType(RecipeType.SHAPELESS)).hasSize(1);
    }

    @Test
    void closesTrackedRecipeRegistrations() {
        var delegate = new FandRecipeRegistry();
        var tracker = new PluginResourceTracker();
        var registry = new PluginRecipeRegistry(delegate, tracker, "demo");
        var registration = registry.register(recipe(Key.key("demo:tracked"), 1));

        tracker.close();

        assertThat(registration.active()).isFalse();
        assertThat(delegate.find(Key.key("demo:tracked"))).isEmpty();
    }

    @Test
    void onlyExposesRecipesOwnedByPluginNamespace() {
        var delegate = new FandRecipeRegistry();
        var registry = new PluginRecipeRegistry(delegate, new PluginResourceTracker(), "demo");
        var owned = recipe(Key.key("demo:owned"), 1);
        var other = recipe(Key.key("other:owned"), 1);
        delegate.register(owned);
        delegate.register(other);

        assertThat(registry.all().stream().map(Recipe::key)).containsExactly(owned.key());
        assertThat(registry.find(Key.key("demo:owned")).map(Recipe::key)).contains(owned.key());
        assertThat(registry.find(Key.key("other:owned")).map(Recipe::key)).contains(owned.key());
        assertThat(registry.remove(Key.key("other:owned"))).isTrue();
        assertThat(delegate.find(Key.key("demo:owned"))).isEmpty();
        assertThat(delegate.find(Key.key("other:owned")).map(Recipe::key)).contains(other.key());
    }

    @Test
    void rejectsRecipeTypesThatCannotBeRegistered() {
        var registry = new PluginRecipeRegistry(new FandRecipeRegistry(), new PluginResourceTracker(), "demo");

        assertThatThrownBy(() -> registry.register(new UnknownRecipe(Key.key("demo:unknown"), ItemStack.EMPTY, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot be registered");
    }

    private static ShapelessRecipe recipe(Key key, int amount) {
        return new ShapelessRecipe(
                key,
                List.of(STONE),
                new ItemStack(DIAMOND, amount));
    }

    private record TestItemType(Key key, int maxStackSize) implements ItemType {
    }
}
