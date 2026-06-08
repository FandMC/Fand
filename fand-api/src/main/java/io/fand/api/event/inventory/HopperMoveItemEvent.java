package io.fand.api.event.inventory;

import io.fand.api.event.Cancellable;
import io.fand.api.event.Event;
import io.fand.api.inventory.Inventory;
import io.fand.api.item.ItemStack;
import io.fand.api.world.Location;
import java.util.Objects;

/**
 * Fired on the server thread before a hopper block or hopper minecart moves an
 * item between inventories.
 */
public final class HopperMoveItemEvent implements Event, Cancellable {

    private final Inventory hopper;
    private final Inventory source;
    private final Inventory destination;
    private final Location hopperLocation;
    private ItemStack item;
    private final boolean hopperInitiated;
    private boolean cancelled;

    public HopperMoveItemEvent(
            Inventory hopper,
            Inventory source,
            Inventory destination,
            Location hopperLocation,
            ItemStack item,
            boolean hopperInitiated
    ) {
        this.hopper = Objects.requireNonNull(hopper, "hopper");
        this.source = Objects.requireNonNull(source, "source");
        this.destination = Objects.requireNonNull(destination, "destination");
        this.hopperLocation = Objects.requireNonNull(hopperLocation, "hopperLocation");
        this.item = Objects.requireNonNull(item, "item");
        this.hopperInitiated = hopperInitiated;
    }

    public Inventory hopper() {
        return hopper;
    }

    public Inventory source() {
        return source;
    }

    public Inventory destination() {
        return destination;
    }

    public Location hopperLocation() {
        return hopperLocation;
    }

    public ItemStack item() {
        return item;
    }

    public void setItem(ItemStack item) {
        this.item = Objects.requireNonNull(item, "item");
    }

    /** True when the hopper is pushing out rather than pulling in. */
    public boolean hopperInitiated() {
        return hopperInitiated;
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
