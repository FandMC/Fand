package io.fand.api.event.inventory;

import io.fand.api.entity.Player;
import io.fand.api.event.Cancellable;
import io.fand.api.event.Event;
import io.fand.api.inventory.Inventory;
import io.fand.api.item.ItemStack;
import java.util.Objects;
import java.util.Set;

/**
 * Fired on the server thread when a player completes a drag operation
 * (releases the mouse after dragging across multiple slots). Cancelling
 * suppresses the placement: the cursor stack stays intact and the dragged
 * slots are left untouched. The client is resynced on the next broadcast.
 *
 * <p>The {@link #slots()} set is in iteration order vanilla recorded the
 * drag — usually but not strictly the order the cursor visited them.
 */
public final class InventoryDragEvent implements Event, Cancellable {

    private final Player player;
    private final Inventory inventory;
    private final DragType dragType;
    private final Set<Integer> slots;
    private final ItemStack cursorItem;
    private boolean cancelled;

    public InventoryDragEvent(
            Player player,
            Inventory inventory,
            DragType dragType,
            Set<Integer> slots,
            ItemStack cursorItem) {
        this.player = Objects.requireNonNull(player, "player");
        this.inventory = Objects.requireNonNull(inventory, "inventory");
        this.dragType = Objects.requireNonNull(dragType, "dragType");
        this.slots = Set.copyOf(Objects.requireNonNull(slots, "slots"));
        this.cursorItem = Objects.requireNonNull(cursorItem, "cursorItem");
    }

    public Player player() {
        return player;
    }

    public Inventory inventory() {
        return inventory;
    }

    public DragType dragType() {
        return dragType;
    }

    /** Slot indices into {@link #inventory()} that the drag would affect. */
    public Set<Integer> slots() {
        return slots;
    }

    /** Cursor stack at the moment the drag completed. */
    public ItemStack cursorItem() {
        return cursorItem;
    }

    @Override
    public boolean cancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }
}
