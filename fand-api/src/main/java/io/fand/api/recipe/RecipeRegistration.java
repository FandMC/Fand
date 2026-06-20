package io.fand.api.recipe;

import net.kyori.adventure.key.Key;

/**
 * Handle returned when a recipe is registered.
 *
 * <p>{@link #unregister()} removes only the recipe this handle installed. If
 * the same key was re-registered after this handle was created, closing this
 * handle must not remove the newer registration; closing an already-closed
 * handle is a no-op.
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
