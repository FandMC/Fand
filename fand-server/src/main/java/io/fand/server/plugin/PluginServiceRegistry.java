package io.fand.server.plugin;

import io.fand.api.service.ServicePriority;
import io.fand.api.service.ServiceProvider;
import io.fand.api.service.ServiceRegistration;
import io.fand.api.service.ServiceRegistry;
import io.fand.server.service.FandServiceRegistry;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import net.kyori.adventure.key.Key;

final class PluginServiceRegistry implements ServiceRegistry {

    private final ServiceRegistry delegate;
    private final PluginResourceTracker tracker;
    private final String owner;

    PluginServiceRegistry(ServiceRegistry delegate, PluginResourceTracker tracker, String owner) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
        this.tracker = Objects.requireNonNull(tracker, "tracker");
        this.owner = Objects.requireNonNull(owner, "owner");
    }

    @Override
    public Collection<ServiceProvider<?>> providers() {
        return delegate.providers();
    }

    @Override
    public <T> Collection<ServiceProvider<T>> providers(Class<T> type) {
        return delegate.providers(type);
    }

    @Override
    public <T> Optional<ServiceProvider<T>> provider(Key key, Class<T> type) {
        return delegate.provider(key, type);
    }

    @Override
    public <T> ServiceRegistration<T> register(Key key, Class<T> type, T service, ServicePriority priority) {
        if (delegate instanceof FandServiceRegistry registry) {
            return tracker.track(registry.register(key, type, service, priority, owner));
        }
        return tracker.track(delegate.register(key, type, service, priority));
    }
}
