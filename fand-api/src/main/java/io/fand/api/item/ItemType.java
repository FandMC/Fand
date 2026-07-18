package io.fand.api.item;

import io.fand.api.item.component.ItemComponents;
import io.fand.api.tag.Tag;
import io.fand.api.tag.Tags;
import java.util.Collection;
import java.util.Objects;
import net.kyori.adventure.key.Key;

/**
 * A vanilla or logical custom item type identified by its registry key.
 *
 * <p>Vanilla types are flyweights resolved from the loaded server registry. Registered
 * custom types are backed by a vanilla base item and components, but retain their own
 * key throughout the Fand API. Obtain either kind through {@link ItemTypes#of(Key)}.
 */
public interface ItemType {

    /** Registry or logical key, e.g. {@code minecraft:diamond} or {@code plugin:ruby}. */
    Key key();

    /** Whether this item type is a member of {@code tag}. */
    default boolean is(Tag<ItemType> tag) {
        return Objects.requireNonNull(tag, "tag").contains(this);
    }

    /** Convenience overload for generated vanilla item tag keys. */
    default boolean is(ItemTagKey tag) {
        return Tags.item(tag).map(candidate -> candidate.contains(this)).orElse(false);
    }

    /** Snapshot of tags currently containing this item type. */
    default Collection<? extends Tag<ItemType>> tags() {
        return Tags.items().stream()
                .filter(tag -> tag.contains(this))
                .toList();
    }

    /** Maximum stack size for this type (1, 16 or 64 for vanilla items). */
    int maxStackSize();

    /** Convenience: builds a stack of this type with {@code amount}. */
    default ItemStack stack(int amount) {
        return new ItemStack(this, amount);
    }

    /** Convenience: builds a stack of this type with {@code amount} and components. */
    default ItemStack stack(int amount, ItemComponents components) {
        return new ItemStack(this, amount, components);
    }

    /** Convenience: builds a stack of this type with one item. */
    default ItemStack one() {
        return stack(1);
    }

    /** Convenience: builds one item with components. */
    default ItemStack one(ItemComponents components) {
        return stack(1, components);
    }
}
