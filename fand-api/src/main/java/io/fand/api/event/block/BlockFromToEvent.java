package io.fand.api.event.block;

import io.fand.api.block.Block;
import io.fand.api.block.BlockType;
import io.fand.api.event.Cancellable;
import io.fand.api.event.Event;
import java.util.Objects;

/**
 * Fired on the server thread before a block-level process moves from one block
 * into another, such as fluid flow.
 */
public final class BlockFromToEvent implements Event, Cancellable {

    private final Block sourceBlock;
    private final Block block;
    private final BlockType sourceType;
    private final BlockType newType;
    private final BlockFace direction;
    private final Cause cause;
    private boolean cancelled;

    public BlockFromToEvent(
            Block sourceBlock,
            Block block,
            BlockType sourceType,
            BlockType newType,
            BlockFace direction,
            Cause cause
    ) {
        this.sourceBlock = Objects.requireNonNull(sourceBlock, "sourceBlock");
        this.block = Objects.requireNonNull(block, "block");
        this.sourceType = Objects.requireNonNull(sourceType, "sourceType");
        this.newType = Objects.requireNonNull(newType, "newType");
        this.direction = Objects.requireNonNull(direction, "direction");
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

    public BlockFace direction() {
        return direction;
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
        FLUID_FLOW,
        UNKNOWN
    }
}
