package io.fand.api.scheduler;

import io.fand.api.world.Chunk;
import io.fand.api.world.Location;
import io.fand.api.world.World;
import java.time.Duration;
import java.util.Objects;
import net.kyori.adventure.key.Key;

/**
 * Schedules background work by world region.
 *
 * <p>Tasks for the same 8x8 chunk region are routed to the same worker lane and
 * run serially. Tasks for different regions may run in parallel when they land
 * on different lanes. Region workers are not the server thread; read or mutate
 * live world state only through thread-safe APIs or hand the mutation back to
 * {@link Scheduler#runMain(Runnable)}.
 */
public interface RegionScheduler {

    int REGION_SIZE_CHUNKS = 8;

    Task run(Key world, int chunkX, int chunkZ, Runnable task);

    Task runAfter(Key world, int chunkX, int chunkZ, Runnable task, Duration delay);

    Task runRepeating(Key world, int chunkX, int chunkZ, Runnable task, Duration initialDelay, Duration period);

    default Task run(World world, int chunkX, int chunkZ, Runnable task) {
        Objects.requireNonNull(world, "world");
        return run(world.key(), chunkX, chunkZ, task);
    }

    default Task run(Chunk chunk, Runnable task) {
        Objects.requireNonNull(chunk, "chunk");
        return run(chunk.world(), chunk.x(), chunk.z(), task);
    }

    default Task run(Location location, Runnable task) {
        Objects.requireNonNull(location, "location");
        return run(location.world(), location.blockX() >> 4, location.blockZ() >> 4, task);
    }

    default Task runAfter(World world, int chunkX, int chunkZ, Runnable task, Duration delay) {
        Objects.requireNonNull(world, "world");
        return runAfter(world.key(), chunkX, chunkZ, task, delay);
    }

    default Task runAfter(Chunk chunk, Runnable task, Duration delay) {
        Objects.requireNonNull(chunk, "chunk");
        return runAfter(chunk.world(), chunk.x(), chunk.z(), task, delay);
    }

    default Task runAfter(Location location, Runnable task, Duration delay) {
        Objects.requireNonNull(location, "location");
        return runAfter(location.world(), location.blockX() >> 4, location.blockZ() >> 4, task, delay);
    }

    default Task runRepeating(
            World world,
            int chunkX,
            int chunkZ,
            Runnable task,
            Duration initialDelay,
            Duration period
    ) {
        Objects.requireNonNull(world, "world");
        return runRepeating(world.key(), chunkX, chunkZ, task, initialDelay, period);
    }

    default Task runRepeating(Chunk chunk, Runnable task, Duration initialDelay, Duration period) {
        Objects.requireNonNull(chunk, "chunk");
        return runRepeating(chunk.world(), chunk.x(), chunk.z(), task, initialDelay, period);
    }

    default Task runRepeating(Location location, Runnable task, Duration initialDelay, Duration period) {
        Objects.requireNonNull(location, "location");
        return runRepeating(
                location.world(),
                location.blockX() >> 4,
                location.blockZ() >> 4,
                task,
                initialDelay,
                period);
    }

    static RegionScheduler unsupported() {
        return UnsupportedRegionScheduler.INSTANCE;
    }
}

enum UnsupportedRegionScheduler implements RegionScheduler {
    INSTANCE;

    @Override
    public Task run(Key world, int chunkX, int chunkZ, Runnable task) {
        throw unsupported();
    }

    @Override
    public Task runAfter(Key world, int chunkX, int chunkZ, Runnable task, Duration delay) {
        throw unsupported();
    }

    @Override
    public Task runRepeating(Key world, int chunkX, int chunkZ, Runnable task, Duration initialDelay, Duration period) {
        throw unsupported();
    }

    private static UnsupportedOperationException unsupported() {
        return new UnsupportedOperationException("Region scheduling is not supported");
    }
}
