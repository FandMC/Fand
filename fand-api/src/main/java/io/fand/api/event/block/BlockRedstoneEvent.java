package io.fand.api.event.block;

import io.fand.api.block.Block;
import io.fand.api.event.Event;
import java.util.Objects;

/**
 * Fired on the server thread before redstone wire applies a new power level.
 */
public final class BlockRedstoneEvent implements Event {

    private final Block block;
    private final int oldCurrent;
    private int newCurrent;

    public BlockRedstoneEvent(Block block, int oldCurrent, int newCurrent) {
        this.block = Objects.requireNonNull(block, "block");
        this.oldCurrent = clamp(oldCurrent);
        this.newCurrent = clamp(newCurrent);
    }

    public Block block() {
        return block;
    }

    public int oldCurrent() {
        return oldCurrent;
    }

    public int newCurrent() {
        return newCurrent;
    }

    public void setNewCurrent(int newCurrent) {
        this.newCurrent = clamp(newCurrent);
    }

    private static int clamp(int current) {
        return Math.max(0, Math.min(15, current));
    }
}
