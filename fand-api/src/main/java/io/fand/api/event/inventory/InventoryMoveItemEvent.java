package io.fand.api.event.inventory;

import io.fand.api.event.Cancellable;
import io.fand.api.event.Event;
import io.fand.api.inventory.Inventory;
import io.fand.api.item.ItemStack;
import java.util.Objects;

/**
 * Fired on the server thread before vanilla automation moves an item stack
 * from one inventory into another.
 */
public final class InventoryMoveItemEvent implements Event, Cancellable {

    private final Inventory source;
    private final Inventory destination;
    private ItemStack item;
    private final boolean sourceInitiated;
    private boolean cancelled;

    public InventoryMoveItemEvent(Inventory source, Inventory destination, ItemStack item, boolean sourceInitiated) {
        this.source = Objects.requireNonNull(source, "source");
        this.destination = Objects.requireNonNull(destination, "destination");
        this.item = Objects.requireNonNull(item, "item");
        this.sourceInitiated = sourceInitiated;
    }

    public Inventory source() {
        return source;
    }

    public Inventory destination() {
        return destination;
    }

    public ItemStack item() {
        return item;
    }

    public void setItem(ItemStack item) {
        this.item = Objects.requireNonNull(item, "item");
    }

    /** True when the source inventory is actively pushing the item. */
    public boolean sourceInitiated() {
        return sourceInitiated;
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
