package io.fand.api.event.inventory;

import io.fand.api.block.Block;
import io.fand.api.event.Cancellable;
import io.fand.api.event.Event;
import io.fand.api.inventory.Inventory;
import io.fand.api.item.ItemStack;
import java.util.Objects;

/**
 * Fired on the server thread before a furnace consumes fuel.
 */
public final class FurnaceBurnEvent implements Event, Cancellable {

    private final Block block;
    private final Inventory inventory;
    private final ItemStack fuel;
    private int burnTime;
    private boolean cancelled;

    public FurnaceBurnEvent(Block block, Inventory inventory, ItemStack fuel, int burnTime) {
        this.block = Objects.requireNonNull(block, "block");
        this.inventory = Objects.requireNonNull(inventory, "inventory");
        this.fuel = Objects.requireNonNull(fuel, "fuel");
        this.burnTime = Math.max(0, burnTime);
    }

    public Block block() {
        return block;
    }

    public Inventory inventory() {
        return inventory;
    }

    public ItemStack fuel() {
        return fuel;
    }

    public int burnTime() {
        return burnTime;
    }

    public void setBurnTime(int burnTime) {
        this.burnTime = Math.max(0, burnTime);
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
