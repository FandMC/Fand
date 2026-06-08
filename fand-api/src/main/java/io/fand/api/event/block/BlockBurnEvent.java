package io.fand.api.event.block;

import io.fand.api.block.Block;
import io.fand.api.block.BlockType;
import io.fand.api.event.Cancellable;
import io.fand.api.event.Event;
import java.util.Objects;

/**
 * Fired on the server thread before fire consumes a flammable block.
 */
public final class BlockBurnEvent implements Event, Cancellable {

    private final Block block;
    private final BlockType blockType;
    private final Block sourceBlock;
    private boolean cancelled;

    public BlockBurnEvent(Block block, BlockType blockType, Block sourceBlock) {
        this.block = Objects.requireNonNull(block, "block");
        this.blockType = Objects.requireNonNull(blockType, "blockType");
        this.sourceBlock = Objects.requireNonNull(sourceBlock, "sourceBlock");
    }

    public Block block() {
        return block;
    }

    public BlockType blockType() {
        return blockType;
    }

    public Block sourceBlock() {
        return sourceBlock;
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
