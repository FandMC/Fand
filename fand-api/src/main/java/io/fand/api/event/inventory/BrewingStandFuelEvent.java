package io.fand.api.event.inventory;

import io.fand.api.block.Block;
import io.fand.api.event.Cancellable;
import io.fand.api.event.Event;
import io.fand.api.inventory.Inventory;
import io.fand.api.item.ItemStack;
import java.util.Objects;

/**
 * Fired on the server thread before a brewing stand consumes fuel.
 *
 * <p>Cancelling prevents the fuel item from being consumed and leaves the
 * stand's fuel counter unchanged. {@link #fuelPower()} is clamped to
 * {@code [0, 127]} because vanilla persists this value as a byte;
 * {@link #consumeAmount()} is mutable and clamped to a non-negative value.
 */
public final class BrewingStandFuelEvent implements Event, Cancellable {

    private final Block block;
    private final Inventory inventory;
    private final ItemStack fuel;
    private int fuelPower;
    private int consumeAmount;
    private boolean cancelled;

    public BrewingStandFuelEvent(Block block, Inventory inventory, ItemStack fuel, int fuelPower, int consumeAmount) {
        this.block = Objects.requireNonNull(block, "block");
        this.inventory = Objects.requireNonNull(inventory, "inventory");
        this.fuel = Objects.requireNonNull(fuel, "fuel");
        this.fuelPower = clampFuelPower(fuelPower);
        this.consumeAmount = Math.max(0, consumeAmount);
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

    public int fuelPower() {
        return fuelPower;
    }

    public void setFuelPower(int fuelPower) {
        this.fuelPower = clampFuelPower(fuelPower);
    }

    public int consumeAmount() {
        return consumeAmount;
    }

    public void setConsumeAmount(int consumeAmount) {
        this.consumeAmount = Math.max(0, consumeAmount);
    }

    @Override
    public boolean cancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    private static int clampFuelPower(int fuelPower) {
        return Math.max(0, Math.min(127, fuelPower));
    }
}
