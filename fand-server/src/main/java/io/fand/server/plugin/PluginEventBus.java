package io.fand.server.plugin;

import io.fand.api.event.Event;
import io.fand.api.event.EventBus;
import io.fand.api.event.EventListener;
import io.fand.api.event.EventPriority;
import io.fand.api.event.EventSubscription;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class PluginEventBus implements EventBus {

    private static final Logger LOGGER = LoggerFactory.getLogger(PluginEventBus.class);

    private final EventBus delegate;
    private final PluginResourceTracker tracker;
    private final String pluginId;

    public PluginEventBus(EventBus delegate, PluginResourceTracker tracker, String pluginId) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
        this.tracker = Objects.requireNonNull(tracker, "tracker");
        this.pluginId = Objects.requireNonNull(pluginId, "pluginId");
    }

    @Override
    public <E extends Event> EventSubscription subscribe(Class<E> type, EventPriority priority, EventListener<E> listener) {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(listener, "listener");
        return tracker.track(delegate.subscribe(type, priority, event -> {
            try {
                listener.on(event);
            } catch (Throwable failure) {
                LOGGER.warn("Plugin {} listener failed for {}", pluginId, type.getName(), failure);
            }
        }));
    }

    @Override
    public <E extends Event> E fire(E event) {
        return delegate.fire(event);
    }

    @Override
    public boolean hasListeners(Class<? extends Event> type) {
        return delegate.hasListeners(type);
    }

    @Override
    public <E extends Event> CompletableFuture<E> fireAsync(E event, Executor executor) {
        return delegate.fireAsync(event, executor);
    }
}
