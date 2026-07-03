package io.fand.api.world;

/**
 * Result summary for a batch chunk operation.
 */
public record ChunkBatchResult(int requested, int succeeded, int skipped, int failed, boolean cancelled) {

    public static ChunkBatchResult empty() {
        return new ChunkBatchResult(0, 0, 0, 0, false);
    }

    public int completed() {
        return succeeded + skipped + failed;
    }

    public int remaining() {
        return Math.max(0, requested - completed());
    }

    public boolean successful() {
        return failed == 0 && !cancelled;
    }
}
