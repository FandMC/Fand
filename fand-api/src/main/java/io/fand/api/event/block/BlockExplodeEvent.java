package io.fand.api.event.block;

import io.fand.api.block.Block;
import io.fand.api.event.Cancellable;
import io.fand.api.event.Event;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Fired on the server thread after an explosion has calculated affected
 * blocks, but before those blocks are broken.
 */
public final class BlockExplodeEvent implements Event, Cancellable {

    private final Block block;
    private final List<Block> affectedBlocks;
    private boolean cancelled;

    public BlockExplodeEvent(Block block, List<? extends Block> affectedBlocks) {
        this.block = Objects.requireNonNull(block, "block");
        this.affectedBlocks = new ArrayList<>(Objects.requireNonNull(affectedBlocks, "affectedBlocks"));
    }

    public Block block() {
        return block;
    }

    /**
     * Live mutable list of blocks vanilla is about to break. Remove entries to
     * protect blocks from this explosion.
     */
    public List<Block> affectedBlocks() {
        return affectedBlocks;
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
