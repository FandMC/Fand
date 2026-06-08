package io.fand.api.event.inventory;

import io.fand.api.block.Block;
import io.fand.api.event.Cancellable;
import io.fand.api.event.Event;
import io.fand.api.inventory.Inventory;
import io.fand.api.item.ItemStack;
import java.util.Objects;

/**
 * Fired on the server thread before a furnace-like block commits a cooked result.
 */
public class BlockCookEvent implements Event, Cancellable {

    private final Block block;
    private final Inventory inventory;
    private final ItemStack source;
    private ItemStack result;
    private boolean cancelled;

    public BlockCookEvent(Block block, Inventory inventory, ItemStack source, ItemStack result) {
        this.block = Objects.requireNonNull(block, "block");
        this.inventory = Objects.requireNonNull(inventory, "inventory");
        this.source = Objects.requireNonNull(source, "source");
        this.result = Objects.requireNonNull(result, "result");
    }

    public Block block() {
        return block;
    }

    public Inventory inventory() {
        return inventory;
    }

    public ItemStack source() {
        return source;
    }

    public ItemStack result() {
        return result;
    }

    public void setResult(ItemStack result) {
        this.result = Objects.requireNonNull(result, "result");
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
