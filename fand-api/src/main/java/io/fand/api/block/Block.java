package io.fand.api.block;

import io.fand.api.world.World;

/**
 * A positional block handle within a {@link World}. Instances are thin handles —
 * {@link #type()} and {@link #setType(BlockType)} read or mutate the live world.
 *
 * <p>Reads must be performed on the main thread; writes always run on the main
 * thread (a write off-thread is silently rescheduled). Equality is by world key
 * plus integer block coordinates.
 */
public interface Block {

    World world();

    int x();

    int y();

    int z();

    /** Current block type at this position. */
    BlockType type();

    /**
     * Replaces the block at this position with {@code type}'s default state and
     * triggers neighbour updates as if a player had placed the block. Returns
     * {@code true} if the world accepted the change.
     */
    boolean setType(BlockType type);
}
