package io.fand.api.recipe;

import io.fand.api.item.ItemType;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import net.kyori.adventure.key.Key;

/**
 * An ingredient accepted by a recipe slot.
 */
public record RecipeIngredient(Optional<Key> tag, List<Key> items) {

    public RecipeIngredient {
        tag = Objects.requireNonNull(tag, "tag");
        items = List.copyOf(Objects.requireNonNull(items, "items"));
        if (tag.isPresent() && !items.isEmpty()) {
            throw new IllegalArgumentException("RecipeIngredient must contain either a tag or items, not both");
        }
        if (tag.isEmpty() && items.isEmpty()) {
            throw new IllegalArgumentException("RecipeIngredient must contain a tag or at least one item");
        }
    }

    public static RecipeIngredient tag(Key tag) {
        return new RecipeIngredient(Optional.of(Objects.requireNonNull(tag, "tag")), List.of());
    }

    public static RecipeIngredient tag(String tag) {
        return tag(Key.key(tag));
    }

    public static RecipeIngredient of(ItemType item) {
        Objects.requireNonNull(item, "item");
        return of(item.key());
    }

    public static RecipeIngredient of(ItemType first, ItemType... rest) {
        Objects.requireNonNull(first, "first");
        Objects.requireNonNull(rest, "rest");
        var items = new java.util.ArrayList<Key>(1 + rest.length);
        items.add(first.key());
        for (var item : rest) {
            items.add(Objects.requireNonNull(item, "item").key());
        }
        return ofKeys(items);
    }

    public static RecipeIngredient of(Key item) {
        return ofKeys(List.of(Objects.requireNonNull(item, "item")));
    }

    public static RecipeIngredient of(String item) {
        return of(Key.key(item));
    }

    public static RecipeIngredient ofKeys(List<Key> items) {
        return new RecipeIngredient(Optional.empty(), items);
    }

    public boolean isTag() {
        return tag.isPresent();
    }
}
