package io.fand.api.event.block;

import io.fand.api.block.Block;
import io.fand.api.event.Cancellable;
import io.fand.api.event.Event;
import java.util.Objects;
import net.kyori.adventure.key.Key;

/**
 * Fired on the server thread before vanilla fluid spreads into a block.
 */
public final class FluidFlowEvent implements Event, Cancellable {

    private final Block sourceBlock;
    private final Block block;
    private final Key fluid;
    private final BlockFace direction;
    private boolean cancelled;

    public FluidFlowEvent(Block sourceBlock, Block block, Key fluid, BlockFace direction) {
        this.sourceBlock = Objects.requireNonNull(sourceBlock, "sourceBlock");
        this.block = Objects.requireNonNull(block, "block");
        this.fluid = Objects.requireNonNull(fluid, "fluid");
        this.direction = Objects.requireNonNull(direction, "direction");
    }

    public Block sourceBlock() {
        return sourceBlock;
    }

    public Block block() {
        return block;
    }

    /** Vanilla fluid registry key, e.g. {@code minecraft:water}. */
    public Key fluid() {
        return fluid;
    }

    public BlockFace direction() {
        return direction;
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
