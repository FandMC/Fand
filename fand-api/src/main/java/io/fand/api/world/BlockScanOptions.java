package io.fand.api.world;

import java.util.Objects;

/**
 * Scheduling policy for block scans that may cover very large regions.
 */
public record BlockScanOptions(
        int maxBlocksPerTick,
        int maxChangesPerBatch,
        boolean loadedChunksOnly,
        BlockBatchOptions batchOptions
) {

    public static final int DEFAULT_MAX_BLOCKS_PER_TICK = 65_536;
    public static final int DEFAULT_MAX_CHANGES_PER_BATCH = 4_096;
    public static final BlockScanOptions DEFAULTS = new BlockScanOptions(
            DEFAULT_MAX_BLOCKS_PER_TICK,
            DEFAULT_MAX_CHANGES_PER_BATCH,
            true,
            BlockBatchOptions.defaults());

    public BlockScanOptions {
        if (maxBlocksPerTick <= 0) {
            throw new IllegalArgumentException("maxBlocksPerTick must be positive");
        }
        if (maxChangesPerBatch <= 0) {
            throw new IllegalArgumentException("maxChangesPerBatch must be positive");
        }
        Objects.requireNonNull(batchOptions, "batchOptions");
    }

    public static BlockScanOptions defaults() {
        return DEFAULTS;
    }

    public BlockScanOptions withMaxBlocksPerTick(int maxBlocksPerTick) {
        return new BlockScanOptions(maxBlocksPerTick, maxChangesPerBatch, loadedChunksOnly, batchOptions);
    }

    public BlockScanOptions withMaxChangesPerBatch(int maxChangesPerBatch) {
        return new BlockScanOptions(maxBlocksPerTick, maxChangesPerBatch, loadedChunksOnly, batchOptions);
    }

    public BlockScanOptions withLoadedChunksOnly(boolean loadedChunksOnly) {
        return new BlockScanOptions(maxBlocksPerTick, maxChangesPerBatch, loadedChunksOnly, batchOptions);
    }

    public BlockScanOptions withBatchOptions(BlockBatchOptions batchOptions) {
        return new BlockScanOptions(maxBlocksPerTick, maxChangesPerBatch, loadedChunksOnly, batchOptions);
    }
}
