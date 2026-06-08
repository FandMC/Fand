package io.fand.api.event.block;

import io.fand.api.block.Block;
import io.fand.api.block.BlockType;
import io.fand.api.event.Cancellable;
import io.fand.api.event.Event;
import java.util.Objects;

/**
 * Fired on the server thread before leaves decay naturally.
 */
public final class LeavesDecayEvent implements Event, Cancellable {

    private final Block block;
    private final BlockType blockType;
    private boolean cancelled;

    public LeavesDecayEvent(Block block, BlockType blockType) {
        this.block = Objects.requireNonNull(block, "block");
        this.blockType = Objects.requireNonNull(blockType, "blockType");
    }

    public Block block() {
        return block;
    }

    public BlockType blockType() {
        return blockType;
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
