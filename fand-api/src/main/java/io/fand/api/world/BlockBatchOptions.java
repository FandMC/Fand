package io.fand.api.world;

import java.util.Objects;

/**
 * Scheduling and update policy for asynchronous block batch operations.
 */
public record BlockBatchOptions(int maxBlocksPerTick, BlockUpdateMode updateMode, boolean skipUnchanged) {

    public static final int DEFAULT_MAX_BLOCKS_PER_TICK = 4096;
    public static final BlockBatchOptions DEFAULTS =
            new BlockBatchOptions(DEFAULT_MAX_BLOCKS_PER_TICK, BlockUpdateMode.NORMAL, true);

    public BlockBatchOptions {
        if (maxBlocksPerTick <= 0) {
            throw new IllegalArgumentException("maxBlocksPerTick must be positive");
        }
        Objects.requireNonNull(updateMode, "updateMode");
    }

    public static BlockBatchOptions defaults() {
        return DEFAULTS;
    }

    public static BlockBatchOptions immediate() {
        return new BlockBatchOptions(Integer.MAX_VALUE, BlockUpdateMode.NORMAL, true);
    }

    public static BlockBatchOptions withoutNeighborUpdates() {
        return new BlockBatchOptions(DEFAULT_MAX_BLOCKS_PER_TICK, BlockUpdateMode.CLIENTS_ONLY, true);
    }

    public BlockBatchOptions withMaxBlocksPerTick(int maxBlocksPerTick) {
        return new BlockBatchOptions(maxBlocksPerTick, updateMode, skipUnchanged);
    }

    public BlockBatchOptions withUpdateMode(BlockUpdateMode updateMode) {
        return new BlockBatchOptions(maxBlocksPerTick, updateMode, skipUnchanged);
    }

    public BlockBatchOptions withSkipUnchanged(boolean skipUnchanged) {
        return new BlockBatchOptions(maxBlocksPerTick, updateMode, skipUnchanged);
    }
}
