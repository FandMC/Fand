package io.fand.api.world;

/**
 * Immutable progress snapshot for a batch chunk operation.
 */
public record ChunkBatchProgress(int requested, int succeeded, int skipped, int failed, boolean cancelled, boolean done) {

    public int completed() {
        return succeeded + skipped + failed;
    }

    public int remaining() {
        return Math.max(0, requested - completed());
    }

    public double ratio() {
        return requested == 0 ? 1.0D : Math.min(1.0D, completed() / (double) requested);
    }
}
