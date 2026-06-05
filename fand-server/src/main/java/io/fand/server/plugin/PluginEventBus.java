package io.fand.server.plugin;

import io.fand.api.event.Event;
import io.fand.api.event.EventBus;
import io.fand.api.event.EventListener;
import io.fand.api.event.EventPriority;
import io.fand.api.event.EventSubscription;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public final class PluginEventBus implements EventBus {

    private final EventBus delegate;
    private final PluginResourceTracker tracker;

    public PluginEventBus(EventBus delegate, PluginResourceTracker tracker) {
        this.delegate = delegate;
        this.tracker = tracker;
    }

    @Override
    public <E extends Event> EventSubscription subscribe(Class<E> type, EventPriority priority, EventListener<E> listener) {
        return tracker.track(delegate.subscribe(type, priority, listener));
    }

    @Override
    public <E extends Event> E fire(E event) {
        return delegate.fire(event);
    }

    @Override
    public <E extends Event> CompletableFuture<E> fireAsync(E event, Executor executor) {
        return delegate.fireAsync(event, executor);
    }
}
