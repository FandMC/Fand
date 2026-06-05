package io.fand.api.inventory;

import io.fand.api.item.ItemStack;

/**
 * A slot-based view over a container. Slot indices are zero-based and contiguous;
 * read/write operations require the server thread.
 *
 * <p>Implementations always return non-null stacks: empty slots read as
 * {@link ItemStack#EMPTY}.
 */
public interface Inventory {

    /** Total number of slots. */
    int size();

    /** Stack in {@code slot}; never {@code null}. */
    ItemStack get(int slot);

    /** Replaces the stack in {@code slot}. Pass {@link ItemStack#EMPTY} to clear. */
    void set(int slot, ItemStack stack);

    /**
     * Tries to add {@code stack} to the first available matching or empty slot.
     * Returns the leftover stack — empty when the entire amount fit.
     */
    ItemStack add(ItemStack stack);

    /** Empties every slot. */
    void clear();
}
