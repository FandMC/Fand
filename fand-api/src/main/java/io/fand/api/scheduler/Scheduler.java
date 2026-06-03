package io.fand.api.scheduler;

import java.time.Duration;

/**
 * Submits tasks to either the main game thread or background workers.
 *
 * <p>Main-thread tasks observe tick boundaries: {@link #runMain(Runnable)} schedules
 * for the next tick, while {@link #runMainAfter(Runnable, Duration)} delays by at
 * least the given duration. Async tasks have no ordering guarantees relative to
 * the main thread.
 */
public interface Scheduler {

    Task runMain(Runnable task);

    Task runMainAfter(Runnable task, Duration delay);

    Task runMainRepeating(Runnable task, Duration initialDelay, Duration period);

    Task runAsync(Runnable task);

    Task runAsyncAfter(Runnable task, Duration delay);
}
