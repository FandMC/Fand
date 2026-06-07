package io.fand.api.recipe;

import io.fand.api.Fand;
import java.util.Collection;
import java.util.Optional;
import net.kyori.adventure.key.Key;

/**
 * Convenience accessor for recipe lookups. Resolves through the currently
 * bound {@link Fand#server()}.
 */
public final class Recipes {

    private Recipes() {
    }

    public static RecipeRegistry registry() {
        return Fand.server().recipes();
    }

    public static Collection<? extends Recipe> all() {
        return registry().all();
    }

    public static Optional<? extends Recipe> find(Key key) {
        return registry().find(key);
    }

    public static Optional<? extends Recipe> find(String key) {
        return find(Key.key(key));
    }
}
