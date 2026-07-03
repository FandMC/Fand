package io.fand.api.world;

/**
 * Receives progress snapshots from a running batch chunk operation.
 *
 * <p>Implementations backed by a live world call this listener on the server
 * thread after each processed slice.
 */
@FunctionalInterface
public interface ChunkBatchListener {

    void onProgress(ChunkBatchProgress progress);
}
