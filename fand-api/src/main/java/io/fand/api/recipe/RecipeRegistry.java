package io.fand.api.recipe;

import java.util.Collection;
import java.util.Optional;
import net.kyori.adventure.key.Key;

/**
 * Registry for server recipes.
 */
public interface RecipeRegistry {

    /** Snapshot of all currently known recipes. */
    Collection<? extends Recipe> all();

    /** Looks up a recipe by key. */
    Optional<? extends Recipe> find(Key key);

    /** Snapshot of all recipes of {@code type}. */
    Collection<? extends Recipe> byType(RecipeType type);

    /**
     * Registers or replaces a recipe.
     *
     * <p>Registration updates the live vanilla recipe manager and returns a
     * handle that removes the same recipe when closed.
     */
    RecipeRegistration register(Recipe recipe);

    /** Removes a recipe by key. Returns {@code true} if a recipe was present. */
    boolean remove(Key key);
}
