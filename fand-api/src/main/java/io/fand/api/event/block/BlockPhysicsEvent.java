package io.fand.api.event.block;

import io.fand.api.block.Block;
import io.fand.api.block.BlockType;
import io.fand.api.event.Cancellable;
import io.fand.api.event.Event;
import java.util.Objects;

/**
 * Fired on the server thread before a block receives a neighbor physics update.
 */
public final class BlockPhysicsEvent implements Event, Cancellable {

    private final Block block;
    private final BlockType sourceType;
    private boolean cancelled;

    public BlockPhysicsEvent(Block block, BlockType sourceType) {
        this.block = Objects.requireNonNull(block, "block");
        this.sourceType = Objects.requireNonNull(sourceType, "sourceType");
    }

    public Block block() {
        return block;
    }

    public BlockType sourceType() {
        return sourceType;
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
