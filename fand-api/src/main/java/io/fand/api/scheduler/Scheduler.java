package io.fand.api.scheduler;

import io.fand.api.entity.Entity;
import io.fand.api.world.Location;
import java.time.Duration;

/**
 * Submits tasks to either the server thread or background workers.
 *
 * <p>Server-thread tasks observe tick boundaries: {@link #runMain(Runnable)} schedules
 * for the next tick, while {@link #runMainAfter(Runnable, Duration)} delays by at
 * least the given duration. Tick-based methods delay by completed server ticks,
 * independent of wall-clock pacing. Async tasks have no ordering guarantees
 * relative to the server thread.
 */
public interface Scheduler {

    /**
     * Targets the owner of a fixed location in this loaded world instance.
     * Does not keep chunks loaded or load them in advance. Reloading a world
     * with the same key does not revive tasks for the previous instance.
     * The scheduler may be obtained and submitted to from any thread.
     */
    default OwnedScheduler at(Location location) {
        throw new UnsupportedOperationException("Location ownership scheduling is not supported");
    }

    /**
     * Targets the entity's owner at execution time, rather than a captured
     * location. A removed entity handle is retired even if its UUID is reused.
     * The scheduler may be obtained and submitted to from any thread.
     */
    default OwnedScheduler forEntity(Entity entity) {
        throw new UnsupportedOperationException("Entity ownership scheduling is not supported");
    }

    /**
     * Targets global server state. This does not grant access to arbitrary
     * world or entity state on servers with multiple execution owners.
     * The scheduler may be obtained and submitted to from any thread.
     */
    default OwnedScheduler global() {
        throw new UnsupportedOperationException("Global ownership scheduling is not supported");
    }

    default RegionScheduler region() {
        return RegionScheduler.unsupported();
    }

    Task runMain(Runnable task);

    Task runMainAfter(Runnable task, Duration delay);

    Task runMainAfterTicks(Runnable task, long delayTicks);

    Task runMainRepeating(Runnable task, Duration initialDelay, Duration period);

    Task runMainRepeatingTicks(Runnable task, long initialDelayTicks, long periodTicks);

    Task runAsync(Runnable task);

    Task runAsyncAfter(Runnable task, Duration delay);
}
