package io.fand.api.event.inventory;

import io.fand.api.entity.ItemEntity;
import io.fand.api.event.Cancellable;
import io.fand.api.event.Event;
import io.fand.api.inventory.Inventory;
import io.fand.api.item.ItemStack;
import io.fand.api.world.Location;
import java.util.Objects;

/**
 * Fired on the server thread before a hopper block or hopper minecart picks up
 * an item entity.
 */
public final class HopperPickupItemEvent implements Event, Cancellable {

    private final Inventory hopper;
    private final Location hopperLocation;
    private final ItemEntity itemEntity;
    private ItemStack item;
    private boolean cancelled;

    public HopperPickupItemEvent(Inventory hopper, Location hopperLocation, ItemEntity itemEntity, ItemStack item) {
        this.hopper = Objects.requireNonNull(hopper, "hopper");
        this.hopperLocation = Objects.requireNonNull(hopperLocation, "hopperLocation");
        this.itemEntity = Objects.requireNonNull(itemEntity, "itemEntity");
        this.item = Objects.requireNonNull(item, "item");
    }

    public Inventory hopper() {
        return hopper;
    }

    public Location hopperLocation() {
        return hopperLocation;
    }

    public ItemEntity itemEntity() {
        return itemEntity;
    }

    public ItemStack item() {
        return item;
    }

    public void setItem(ItemStack item) {
        this.item = Objects.requireNonNull(item, "item");
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
