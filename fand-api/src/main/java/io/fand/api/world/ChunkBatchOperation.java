package io.fand.api.world;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Handle for a running batch chunk operation.
 */
public interface ChunkBatchOperation {

    CompletableFuture<ChunkBatchResult> future();

    ChunkBatchProgress progress();

    boolean cancel();

    /**
     * Registers a progress listener. Implementations backed by a live world
     * invoke it on the server thread after processed slices.
     */
    default ChunkBatchOperation onProgress(ChunkBatchListener listener) {
        java.util.Objects.requireNonNull(listener, "listener");
        try {
            listener.onProgress(progress());
        } catch (RuntimeException ignored) {
            // Progress observers must not affect the operation itself.
        }
        return this;
    }

    default boolean done() {
        return future().isDone();
    }

    default ChunkBatchResult join() {
        return future().join();
    }

    default ChunkBatchOperation onComplete(Consumer<? super ChunkBatchResult> action) {
        java.util.Objects.requireNonNull(action, "action");
        future().thenAccept(action);
        return this;
    }
}
