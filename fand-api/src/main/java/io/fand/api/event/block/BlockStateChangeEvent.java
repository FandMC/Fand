package io.fand.api.event.block;

import io.fand.api.block.Block;
import io.fand.api.block.BlockStateSnapshot;
import io.fand.api.event.Cancellable;
import io.fand.api.event.Event;
import java.util.Objects;

/**
 * Fired on the server thread before any block state change is committed.
 *
 * <p>Unlike {@link BlockChangeEvent}, this event also covers changes where the
 * block type remains the same and only one or more state properties change.
 * It is a hot, low-level event; listeners should keep work bounded.
 */
public final class BlockStateChangeEvent implements Event, Cancellable {

    private final Block block;
    private final BlockStateSnapshot oldState;
    private final BlockStateSnapshot newState;
    private final int updateFlags;
    private boolean cancelled;

    public BlockStateChangeEvent(
            Block block,
            BlockStateSnapshot oldState,
            BlockStateSnapshot newState,
            int updateFlags
    ) {
        this.block = Objects.requireNonNull(block, "block");
        this.oldState = Objects.requireNonNull(oldState, "oldState");
        this.newState = Objects.requireNonNull(newState, "newState");
        this.updateFlags = updateFlags;
    }

    public Block block() {
        return block;
    }

    public BlockStateSnapshot oldState() {
        return oldState;
    }

    public BlockStateSnapshot newState() {
        return newState;
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
