package io.fand.api.world;

/**
 * Result counters for a completed block batch operation.
 */
public record BlockBatchResult(int requested, int changed, int skipped, int failed) {

    public BlockBatchResult {
        if (requested < 0 || changed < 0 || skipped < 0 || failed < 0) {
            throw new IllegalArgumentException("batch result counters must not be negative");
        }
        long completed = (long) changed + skipped + failed;
        if (completed > requested) {
            throw new IllegalArgumentException("changed + skipped + failed cannot exceed requested");
        }
    }

    public static BlockBatchResult empty() {
        return new BlockBatchResult(0, 0, 0, 0);
    }
}
