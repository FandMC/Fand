package io.fand.api.world;

import java.util.Objects;

/**
 * Scheduling policy for fluid scans that may cover many blocks.
 */
public record FluidBatchOptions(
        int maxBlocksPerTick,
        int maxChangesPerBatch,
        boolean loadedChunksOnly,
        BlockBatchOptions batchOptions
) {

    public static final int DEFAULT_MAX_BLOCKS_PER_TICK = 65_536;
    public static final int DEFAULT_MAX_CHANGES_PER_BATCH = 4_096;
    public static final FluidBatchOptions DEFAULTS = new FluidBatchOptions(
            DEFAULT_MAX_BLOCKS_PER_TICK,
            DEFAULT_MAX_CHANGES_PER_BATCH,
            true,
            BlockBatchOptions.defaults().withUpdateMode(BlockUpdateMode.CLIENTS_ONLY));

    public FluidBatchOptions {
        if (maxBlocksPerTick <= 0) {
            throw new IllegalArgumentException("maxBlocksPerTick must be positive");
        }
        if (maxChangesPerBatch <= 0) {
            throw new IllegalArgumentException("maxChangesPerBatch must be positive");
        }
        Objects.requireNonNull(batchOptions, "batchOptions");
    }

    public static FluidBatchOptions defaults() {
        return DEFAULTS;
    }

    public FluidBatchOptions withMaxBlocksPerTick(int maxBlocksPerTick) {
        return new FluidBatchOptions(maxBlocksPerTick, maxChangesPerBatch, loadedChunksOnly, batchOptions);
    }

    public FluidBatchOptions withMaxChangesPerBatch(int maxChangesPerBatch) {
        return new FluidBatchOptions(maxBlocksPerTick, maxChangesPerBatch, loadedChunksOnly, batchOptions);
    }

    public FluidBatchOptions withLoadedChunksOnly(boolean loadedChunksOnly) {
        return new FluidBatchOptions(maxBlocksPerTick, maxChangesPerBatch, loadedChunksOnly, batchOptions);
    }

    public FluidBatchOptions withBatchOptions(BlockBatchOptions batchOptions) {
        return new FluidBatchOptions(maxBlocksPerTick, maxChangesPerBatch, loadedChunksOnly, batchOptions);
    }
}
