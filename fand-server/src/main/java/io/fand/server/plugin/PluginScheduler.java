package io.fand.server.plugin;

import io.fand.api.scheduler.Scheduler;
import io.fand.api.scheduler.Task;
import java.time.Duration;

public final class PluginScheduler implements Scheduler {

    private final Scheduler delegate;
    private final PluginResourceTracker tracker;

    public PluginScheduler(Scheduler delegate, PluginResourceTracker tracker) {
        this.delegate = delegate;
        this.tracker = tracker;
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
}
