package io.fand.api.event.block;

import io.fand.api.block.Block;
import io.fand.api.block.BlockType;
import io.fand.api.event.Cancellable;
import io.fand.api.event.Event;
import java.util.Objects;
import java.util.Optional;

/**
 * Fired on the server thread before a fire block is created.
 */
public final class BlockIgniteEvent implements Event, Cancellable {

    private final Block block;
    private final BlockType ignitedType;
    private final Cause cause;
    private final Optional<Block> sourceBlock;
    private boolean cancelled;

    public BlockIgniteEvent(Block block, BlockType ignitedType, Cause cause, Optional<Block> sourceBlock) {
        this.block = Objects.requireNonNull(block, "block");
        this.ignitedType = Objects.requireNonNull(ignitedType, "ignitedType");
        this.cause = Objects.requireNonNull(cause, "cause");
        this.sourceBlock = Objects.requireNonNull(sourceBlock, "sourceBlock");
    }

    public Block block() {
        return block;
    }

    public BlockType ignitedType() {
        return ignitedType;
    }

    public Cause cause() {
        return cause;
    }

    public Optional<Block> sourceBlock() {
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

    public enum Cause {
        SPREAD,
        UNKNOWN
    }
}
