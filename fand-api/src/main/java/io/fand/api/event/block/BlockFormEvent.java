package io.fand.api.event.block;

import io.fand.api.block.Block;
import io.fand.api.block.BlockType;
import io.fand.api.event.Cancellable;
import io.fand.api.event.Event;
import java.util.Objects;

/**
 * Fired on the server thread before a natural process forms a block.
 */
public final class BlockFormEvent implements Event, Cancellable {

    private final Block block;
    private final BlockType oldType;
    private final BlockType newType;
    private final Cause cause;
    private boolean cancelled;

    public BlockFormEvent(Block block, BlockType oldType, BlockType newType, Cause cause) {
        this.block = Objects.requireNonNull(block, "block");
        this.oldType = Objects.requireNonNull(oldType, "oldType");
        this.newType = Objects.requireNonNull(newType, "newType");
        this.cause = Objects.requireNonNull(cause, "cause");
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
        LAVA_INTERACTION,
        FROST,
        SNOW,
        DRIPSTONE,
        UNKNOWN
    }
}
