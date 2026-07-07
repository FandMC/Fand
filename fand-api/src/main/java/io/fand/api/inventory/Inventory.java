package io.fand.api.inventory;

import io.fand.api.entity.Player;
import io.fand.api.item.ItemStack;
import io.fand.api.item.ItemType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
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

    /** Snapshot of every slot, preserving slot order. */
    default List<ItemStack> contents() {
        var contents = new ArrayList<ItemStack>(size());
        for (var slot = 0; slot < size(); slot++) {
            contents.add(get(slot));
        }
        return List.copyOf(contents);
    }

    /**
     * Replaces contents from slot {@code 0}; slots past {@code contents} are
     * cleared. Extra entries are ignored.
     */
    default void setContents(Collection<ItemStack> contents) {
        Objects.requireNonNull(contents, "contents");
        var slot = 0;
        for (var stack : contents) {
            if (slot >= size()) {
                return;
            }
            set(slot++, Objects.requireNonNull(stack, "stack"));
        }
        while (slot < size()) {
            set(slot++, ItemStack.EMPTY);
        }
    }

    /** Convenience overload for replacing contents from an array. */
    default void setContents(ItemStack... contents) {
        setContents(List.of(contents));
    }

    /** Returns the first empty slot, or {@code -1} when the inventory is full. */
    default int firstEmpty() {
        for (var slot = 0; slot < size(); slot++) {
            if (get(slot).empty()) {
                return slot;
            }
        }
        return -1;
    }

    /** Returns whether every slot is empty. */
    default boolean empty() {
        for (var slot = 0; slot < size(); slot++) {
            if (!get(slot).empty()) {
                return false;
            }
        }
        return true;
    }

    /** Counts all stacks with the given type, ignoring data components. */
    default int count(ItemType type) {
        Objects.requireNonNull(type, "type");
        var count = 0;
        for (var slot = 0; slot < size(); slot++) {
            var stack = get(slot);
            if (!stack.empty() && type.equals(stack.type())) {
                count += stack.amount();
            }
        }
        return count;
    }

    /** Counts stacks that match both item type and components. */
    default int count(ItemStack stack) {
        Objects.requireNonNull(stack, "stack");
        if (stack.empty()) {
            return 0;
        }
        var count = 0;
        for (var slot = 0; slot < size(); slot++) {
            var current = get(slot);
            if (sameItem(current, stack)) {
                count += current.amount();
            }
        }
        return count;
    }

    /** Returns true when at least one item of {@code type} exists. */
    default boolean contains(ItemType type) {
        return count(type) > 0;
    }

    /** Returns true when the inventory contains at least {@code stack.amount()} matching items. */
    default boolean contains(ItemStack stack) {
        Objects.requireNonNull(stack, "stack");
        return !stack.empty() && count(stack) >= stack.amount();
    }

    /** Removes up to {@code amount} items of {@code type}; returns the number removed. */
    default int remove(ItemType type, int amount) {
        Objects.requireNonNull(type, "type");
        if (amount <= 0) {
            return 0;
        }
        return removeMatching(type, amount, false, ItemStack.EMPTY);
    }

    /** Removes up to {@code amount} items matching {@code stack}'s type and components. */
    default int remove(ItemStack stack, int amount) {
        Objects.requireNonNull(stack, "stack");
        if (stack.empty() || amount <= 0) {
            return 0;
        }
        return removeMatching(stack.type(), amount, true, stack);
    }

    /** Removes up to {@code stack.amount()} matching items. */
    default int remove(ItemStack stack) {
        Objects.requireNonNull(stack, "stack");
        return remove(stack, stack.amount());
    }

    /** Current viewers of this inventory. Inventories without viewer tracking return an empty snapshot. */
    default Collection<? extends Player> viewers() {
        return List.of();
    }

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

    private int removeMatching(ItemType type, int amount, boolean exact, ItemStack expected) {
        var remaining = amount;
        for (var slot = 0; slot < size() && remaining > 0; slot++) {
            var current = get(slot);
            if (current.empty() || !type.equals(current.type()) || exact && !sameItem(current, expected)) {
                continue;
            }
            var removed = Math.min(remaining, current.amount());
            var leftover = current.amount() - removed;
            set(slot, leftover == 0 ? ItemStack.EMPTY : current.withAmount(leftover));
            remaining -= removed;
        }
        return amount - remaining;
    }

    private static boolean sameItem(ItemStack first, ItemStack second) {
        if (first.empty() || second.empty()) {
            return false;
        }
        return Objects.equals(first.type(), second.type())
                && first.components().equals(second.components());
    }
}
