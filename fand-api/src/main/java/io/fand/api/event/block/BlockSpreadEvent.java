package io.fand.api.event.block;

import io.fand.api.block.Block;
import io.fand.api.block.BlockType;
import io.fand.api.event.Cancellable;
import io.fand.api.event.Event;
import java.util.Objects;

/**
 * Fired on the server thread before a block spreads into another position.
 */
public final class BlockSpreadEvent implements Event, Cancellable {

    private final Block sourceBlock;
    private final Block block;
    private final BlockType sourceType;
    private final BlockType newType;
    private final Cause cause;
    private boolean cancelled;

    public BlockSpreadEvent(Block sourceBlock, Block block, BlockType sourceType, BlockType newType, Cause cause) {
        this.sourceBlock = Objects.requireNonNull(sourceBlock, "sourceBlock");
        this.block = Objects.requireNonNull(block, "block");
        this.sourceType = Objects.requireNonNull(sourceType, "sourceType");
        this.newType = Objects.requireNonNull(newType, "newType");
        this.cause = Objects.requireNonNull(cause, "cause");
    }

    public Block sourceBlock() {
        return sourceBlock;
    }

    public Block block() {
        return block;
    }

    public BlockType sourceType() {
        return sourceType;
    }

    public BlockType newType() {
        return newType;
    }

    public Cause cause() {
        return cause;
    }

    @Override
    public boolean cancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    public enum Cause {
        FIRE,
        SNOWY_BLOCK,
        UNKNOWN
    }
}
