package io.fand.api.event.inventory;

import io.fand.api.block.Block;
import io.fand.api.event.Cancellable;
import io.fand.api.event.Event;
import io.fand.api.inventory.Inventory;
import io.fand.api.item.ItemStack;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Fired on the server thread before a brewing stand commits brewed results.
 */
public final class BrewEvent implements Event, Cancellable {

    private final Block block;
    private final Inventory inventory;
    private final ItemStack ingredient;
    private final List<ItemStack> results;
    private boolean cancelled;

    public BrewEvent(Block block, Inventory inventory, ItemStack ingredient, List<ItemStack> results) {
        this.block = Objects.requireNonNull(block, "block");
        this.inventory = Objects.requireNonNull(inventory, "inventory");
        this.ingredient = Objects.requireNonNull(ingredient, "ingredient");
        this.results = new ArrayList<>(Objects.requireNonNull(results, "results"));
    }

    public Block block() {
        return block;
    }

    public Inventory inventory() {
        return inventory;
    }

    public ItemStack ingredient() {
        return ingredient;
    }

    public List<ItemStack> results() {
        return results;
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
