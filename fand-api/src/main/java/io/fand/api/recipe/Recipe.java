package io.fand.api.recipe;

import io.fand.api.item.ItemStack;
import java.util.Optional;
import net.kyori.adventure.key.Key;

/**
 * Base view of a server recipe.
 */
public interface Recipe {

    /** Registry key, e.g. {@code fand:example_sword}. */
    Key key();

    /** Coarse recipe family. */
    RecipeType type();

    /** Optional recipe book grouping string. */
    Optional<String> group();

    /** Primary result stack. */
    ItemStack result();
}
