package io.fand.api.scheduler;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * Schedules one-shot work in the execution context owning a location, an entity,
 * or global server state. Submissions may originate on any thread and never run
 * inline; they execute on a subsequent owner tick.
 *
 * <p>The target is checked immediately before execution. A retired entity or
 * unloaded world fails the future with {@link IllegalStateException}. Closing
 * the scheduler or disabling the submitting plugin cancels pending work.
 * Cancellation is best-effort and cannot undo an action that has started.
 *
 * <p>Future continuations have no ownership or thread guarantee. Schedule any
 * subsequent world access through the appropriate owner again. Returned values
 * must be safe to use on the receiving thread; returning a live handle does not
 * make its state thread-safe.
 */
public interface OwnedScheduler {

    /**
     * Submits a query or action. Scheduling rejection and action failure complete
     * the future exceptionally; cancellation never interrupts a tick thread.
     */
    <T> CompletableFuture<T> call(Supplier<T> action);

    /** Submits an action with the same execution and cancellation rules as {@link #call(Supplier)}. */
    default CompletableFuture<Void> run(Runnable action) {
        Objects.requireNonNull(action, "action");
        return call(() -> {
            action.run();
            return null;
        });
    }
}
