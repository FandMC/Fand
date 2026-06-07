package io.fand.api.recipe;

import net.kyori.adventure.key.Key;

/**
 * Handle returned when a recipe is registered.
 */
public interface RecipeRegistration extends AutoCloseable {

    Key key();

    boolean active();

    void unregister();

    @Override
    default void close() {
        unregister();
    }
}
