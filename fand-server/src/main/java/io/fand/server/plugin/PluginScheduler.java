package io.fand.server.plugin;

import io.fand.api.entity.Entity;
import io.fand.api.scheduler.OwnedScheduler;
import io.fand.api.scheduler.RegionScheduler;
import io.fand.api.scheduler.Scheduler;
import io.fand.api.scheduler.Task;
import io.fand.api.world.Location;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import net.kyori.adventure.key.Key;

public final class PluginScheduler implements Scheduler {

    private final Scheduler delegate;
    private final PluginResourceTracker tracker;

    public PluginScheduler(Scheduler delegate, PluginResourceTracker tracker) {
        this.delegate = delegate;
        this.tracker = tracker;
    }

    @Override
    public OwnedScheduler at(Location location) {
        return new TrackedOwnedScheduler(delegate.at(location));
    }

    @Override
    public OwnedScheduler forEntity(Entity entity) {
        return new TrackedOwnedScheduler(delegate.forEntity(entity));
    }

    @Override
    public OwnedScheduler global() {
        return new TrackedOwnedScheduler(delegate.global());
    }

    @Override
    public RegionScheduler region() {
        return new TrackedRegionScheduler(delegate.region());
    }

    @Override
    public Task runMain(Runnable task) {
        return tracker.track(delegate.runMain(task));
    }

    @Override
    public Task runMainAfter(Runnable task, Duration delay) {
        return tracker.track(delegate.runMainAfter(task, delay));
    }

    @Override
    public Task runMainAfterTicks(Runnable task, long delayTicks) {
        return tracker.track(delegate.runMainAfterTicks(task, delayTicks));
    }

    @Override
    public Task runMainRepeating(Runnable task, Duration initialDelay, Duration period) {
        return tracker.track(delegate.runMainRepeating(task, initialDelay, period));
    }

    @Override
    public Task runMainRepeatingTicks(Runnable task, long initialDelayTicks, long periodTicks) {
        return tracker.track(delegate.runMainRepeatingTicks(task, initialDelayTicks, periodTicks));
    }

    @Override
    public Task runAsync(Runnable task) {
        return tracker.track(delegate.runAsync(task));
    }

    @Override
    public Task runAsyncAfter(Runnable task, Duration delay) {
        return tracker.track(delegate.runAsyncAfter(task, delay));
    }

    private final class TrackedOwnedScheduler implements OwnedScheduler {

        private final OwnedScheduler delegate;

        private TrackedOwnedScheduler(OwnedScheduler delegate) {
            this.delegate = delegate;
        }

        @Override
        public <T> CompletableFuture<T> call(Supplier<T> action) {
            Objects.requireNonNull(action, "action");
            return tracker.trackCall(() -> delegate.call(() -> tracker.callIfOpen(action)));
        }
    }

    private final class TrackedRegionScheduler implements RegionScheduler {

        private final RegionScheduler delegate;

        private TrackedRegionScheduler(RegionScheduler delegate) {
            this.delegate = delegate;
        }

        @Override
        public Task run(Key world, int chunkX, int chunkZ, Runnable task) {
            return tracker.track(delegate.run(world, chunkX, chunkZ, task));
        }

        @Override
        public Task runAfter(Key world, int chunkX, int chunkZ, Runnable task, Duration delay) {
            return tracker.track(delegate.runAfter(world, chunkX, chunkZ, task, delay));
        }

        @Override
        public Task runRepeating(Key world, int chunkX, int chunkZ, Runnable task, Duration initialDelay, Duration period) {
            return tracker.track(delegate.runRepeating(world, chunkX, chunkZ, task, initialDelay, period));
        }
    }
}
