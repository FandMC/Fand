package io.fand.api.item;

import io.fand.api.item.component.ItemComponents;
import net.kyori.adventure.key.Key;

/**
 * A Minecraft item type identified by its registry key (e.g. {@code minecraft:stone}).
 *
 * <p>Types are flyweights resolved from the loaded server registry; obtain instances
 * via {@link ItemTypes#of(Key)}.
 */
public interface ItemType {

    /** Registry key, e.g. {@code minecraft:diamond}. */
    Key key();

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
