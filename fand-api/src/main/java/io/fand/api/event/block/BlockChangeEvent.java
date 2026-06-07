package io.fand.api.event.block;

import io.fand.api.block.Block;
import io.fand.api.block.BlockType;
import io.fand.api.event.Cancellable;
import io.fand.api.event.Event;
import java.util.Objects;

/**
 * Fired on the server thread before a block state change is committed.
 *
 * <p>This is a low-level event and may accompany higher-level events such as
 * {@link BlockPlaceEvent} and {@link BlockBreakEvent}. It also covers world
 * mutations from ticking blocks, fluids, explosions, redstone, commands, and
 * plugin calls.
 */
public final class BlockChangeEvent implements Event, Cancellable {

    private final Block block;
    private final BlockType oldType;
    private final BlockType newType;
    private final int updateFlags;
    private boolean cancelled;

    public BlockChangeEvent(Block block, BlockType oldType, BlockType newType, int updateFlags) {
        this.block = Objects.requireNonNull(block, "block");
        this.oldType = Objects.requireNonNull(oldType, "oldType");
        this.newType = Objects.requireNonNull(newType, "newType");
        this.updateFlags = updateFlags;
    }

    public Block block() {
        return block;
    }

    public BlockType oldType() {
        return oldType;
    }

    public BlockType newType() {
        return newType;
    }

    public int updateFlags() {
        return updateFlags;
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
