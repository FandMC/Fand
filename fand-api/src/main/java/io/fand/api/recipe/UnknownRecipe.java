package io.fand.api.recipe;

import io.fand.api.item.ItemStack;
import java.util.Objects;
import java.util.Optional;
import net.kyori.adventure.key.Key;
import org.jspecify.annotations.Nullable;

/**
 * Read-only view of a vanilla recipe shape Fand does not expose for registration yet.
 */
public record UnknownRecipe(Key key, ItemStack result, @Nullable String groupName) implements Recipe {

    public UnknownRecipe {
        key = Objects.requireNonNull(key, "key");
        result = result == null ? ItemStack.EMPTY : result;
        groupName = ShapedRecipe.normalizeGroup(groupName);
    }

    @Override
    public RecipeType type() {
        return RecipeType.UNKNOWN;
    }

    @Override
    public Optional<String> group() {
        return Optional.ofNullable(groupName);
    }
}
