package io.fand.api.event.block;

import io.fand.api.block.Block;
import io.fand.api.event.Cancellable;
import io.fand.api.event.Event;
import io.fand.api.item.ItemStack;
import java.util.Objects;

/**
 * Fired on the server thread before a dispenser or dropper dispenses an item.
 */
public final class BlockDispenseEvent implements Event, Cancellable {

    private final Block block;
    private final BlockFace direction;
    private ItemStack item;
    private boolean cancelled;

    public BlockDispenseEvent(Block block, BlockFace direction, ItemStack item) {
        this.block = Objects.requireNonNull(block, "block");
        this.direction = Objects.requireNonNull(direction, "direction");
        this.item = Objects.requireNonNull(item, "item");
    }

    public Block block() {
        return block;
    }

    public BlockFace direction() {
        return direction;
    }

    /** Single item stack vanilla is about to dispense from the source block. */
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
