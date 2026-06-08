package io.fand.api.event.block;

import io.fand.api.block.Block;
import io.fand.api.event.Cancellable;
import io.fand.api.event.Event;
import java.util.List;
import java.util.Objects;

/**
 * Fired on the server thread before a piston extends and moves blocks.
 */
public class BlockPistonExtendEvent implements Event, Cancellable {

    private final Block block;
    private final BlockFace direction;
    private final List<Block> affectedBlocks;
    private boolean cancelled;

    public BlockPistonExtendEvent(Block block, BlockFace direction, List<Block> affectedBlocks) {
        this.block = Objects.requireNonNull(block, "block");
        this.direction = Objects.requireNonNull(direction, "direction");
        this.affectedBlocks = List.copyOf(Objects.requireNonNull(affectedBlocks, "affectedBlocks"));
    }

    public Block block() {
        return block;
    }

    public BlockFace direction() {
        return direction;
    }

    /** Blocks vanilla plans to push or destroy for this piston action. */
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
