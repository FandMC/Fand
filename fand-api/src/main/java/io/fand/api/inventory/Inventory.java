package io.fand.api.inventory;

import io.fand.api.item.ItemStack;
import net.kyori.adventure.text.Component;
import org.jspecify.annotations.Nullable;

/**
 * A slot-based view over a container. Slot indices are zero-based and contiguous;
 * read/write operations require the server thread.
 *
 * <p>Implementations always return non-null stacks: empty slots read as
 * {@link ItemStack#EMPTY}.
 */
public interface Inventory {

    /** Coarse type classification — what kind of container this is. */
    InventoryType type();

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

    /**
     * Title shown to viewers, or {@code null} if this inventory has no
     * dedicated title (e.g. the player's own inventory or a vanilla menu
     * surfaced through events).
     */
    default @Nullable Component title() {
        return null;
    }

    /**
     * Registers {@code listener} for slot mutations. Only supported by
     * inventories created via {@link Inventories#create}; default
     * implementation throws {@link UnsupportedOperationException}.
     *
     * @return a handle that, when {@link AutoCloseable#close() closed},
     *         removes the listener
     */
    default AutoCloseable addSlotChangeListener(SlotChangeListener listener) {
        throw new UnsupportedOperationException(
                "This inventory does not support slot-change listeners");
    }
}
