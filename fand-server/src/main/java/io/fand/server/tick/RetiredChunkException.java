package io.fand.server.tick;

import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;

/** A lifecycle cancellation, not a damaged chunk or a failed generation step. */
public final class RetiredChunkException extends CancellationException {
    public RetiredChunkException() {
        super("Chunk-holder lifetime has ended");
    }

    public static boolean isRetirement(Throwable failure) {
        // Java 25 get/join wraps cancellation to retain the waiting caller's stack.
        while ((failure instanceof CompletionException || failure instanceof ExecutionException
                || failure instanceof CancellationException && !(failure instanceof RetiredChunkException))
                && failure.getCause() != null) {
            failure = failure.getCause();
        }
        return failure instanceof RetiredChunkException;
    }
}
