package io.fand.api.recipe;

import io.fand.api.item.ItemStack;
import java.util.Objects;
import java.util.Optional;
import net.kyori.adventure.key.Key;
import org.jspecify.annotations.Nullable;

/**
 * Read-only view of a vanilla special/complex recipe such as firework, banner,
 * map, book, or repair recipes.
 */
public record ComplexRecipe(
        Key key,
        Key serializer,
        ItemStack result,
        @Nullable String groupName
) implements Recipe {

    public ComplexRecipe(Key key, Key serializer, ItemStack result) {
        this(key, serializer, result, null);
    }

    public ComplexRecipe {
        key = Objects.requireNonNull(key, "key");
        serializer = Objects.requireNonNull(serializer, "serializer");
        result = result == null ? ItemStack.EMPTY : result;
        groupName = ShapedRecipe.normalizeGroup(groupName);
    }

    @Override
    public RecipeType type() {
        return RecipeType.COMPLEX;
    }

    @Override
    public Optional<String> group() {
        return Optional.ofNullable(groupName);
    }
}
