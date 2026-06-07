package io.fand.server.plugin;

import io.fand.api.recipe.CookingRecipe;
import io.fand.api.recipe.Recipe;
import io.fand.api.recipe.RecipeRegistration;
import io.fand.api.recipe.RecipeRegistry;
import io.fand.api.recipe.RecipeType;
import io.fand.api.recipe.ShapedRecipe;
import io.fand.api.recipe.ShapelessRecipe;
import io.fand.api.recipe.StonecuttingRecipe;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import net.kyori.adventure.key.Key;

public final class PluginRecipeRegistry implements RecipeRegistry {

    private final RecipeRegistry delegate;
    private final PluginResourceTracker tracker;
    private final String namespace;

    public PluginRecipeRegistry(RecipeRegistry delegate, PluginResourceTracker tracker, String namespace) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
        this.tracker = Objects.requireNonNull(tracker, "tracker");
        this.namespace = Objects.requireNonNull(namespace, "namespace");
    }

    public String namespace() {
        return namespace;
    }

    @Override
    public Collection<? extends Recipe> all() {
        return delegate.all().stream()
                .filter(this::ownedByThisPlugin)
                .toList();
    }

    @Override
    public Optional<? extends Recipe> find(Key key) {
        return delegate.find(scopedKey(key)).filter(this::ownedByThisPlugin);
    }

    @Override
    public Collection<? extends Recipe> byType(RecipeType type) {
        return delegate.byType(type).stream()
                .filter(this::ownedByThisPlugin)
                .toList();
    }

    @Override
    public RecipeRegistration register(Recipe recipe) {
        return tracker.track(delegate.register(scope(recipe)));
    }

    @Override
    public boolean remove(Key key) {
        return delegate.remove(scopedKey(key));
    }

    private Recipe scope(Recipe recipe) {
        return switch (Objects.requireNonNull(recipe, "recipe")) {
            case ShapedRecipe shaped -> new ShapedRecipe(
                    scopedKey(shaped.key()),
                    shaped.pattern(),
                    shaped.ingredients(),
                    shaped.result(),
                    shaped.groupName(),
                    shaped.category(),
                    shaped.showNotification()
            );
            case ShapelessRecipe shapeless -> new ShapelessRecipe(
                    scopedKey(shapeless.key()),
                    shapeless.ingredients(),
                    shapeless.result(),
                    shapeless.groupName(),
                    shapeless.category(),
                    shapeless.showNotification()
            );
            case CookingRecipe cooking -> new CookingRecipe(
                    scopedKey(cooking.key()),
                    cooking.type(),
                    cooking.ingredient(),
                    cooking.result(),
                    cooking.experience(),
                    cooking.cookingTimeTicks(),
                    cooking.groupName(),
                    cooking.category(),
                    cooking.showNotification()
            );
            case StonecuttingRecipe stonecutting -> new StonecuttingRecipe(
                    scopedKey(stonecutting.key()),
                    stonecutting.ingredient(),
                    stonecutting.result(),
                    stonecutting.groupName(),
                    stonecutting.showNotification()
            );
            default -> throw new IllegalArgumentException("Recipe type cannot be registered: " + recipe.type());
        };
    }

    private Key scopedKey(Key key) {
        Objects.requireNonNull(key, "key");
        return Key.key(namespace, key.value());
    }

    private boolean ownedByThisPlugin(Recipe recipe) {
        return namespace.equals(recipe.key().namespace());
    }
}
