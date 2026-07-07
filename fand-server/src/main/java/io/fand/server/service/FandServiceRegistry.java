package io.fand.server.service;

import io.fand.api.service.ServicePriority;
import io.fand.api.service.ServiceProvider;
import io.fand.api.service.ServiceRegistration;
import io.fand.api.service.ServiceRegistry;
import io.fand.server.permission.PermissionManager;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import net.kyori.adventure.key.Key;
import org.jspecify.annotations.Nullable;

public final class FandServiceRegistry implements ServiceRegistry, AutoCloseable {

    private final Object lock = new Object();
    private final LinkedHashMap<ServiceId, Registration<?>> registrations = new LinkedHashMap<>();
    private final AtomicLong sequence = new AtomicLong();
    private final @Nullable PermissionManager permissions;

    public FandServiceRegistry() {
        this(null);
    }

    public FandServiceRegistry(@Nullable PermissionManager permissions) {
        this.permissions = permissions;
    }

    @Override
    public Collection<ServiceProvider<?>> providers() {
        synchronized (lock) {
            var providers = new ArrayList<ServiceProvider<?>>();
            registrations.values().stream()
                    .filter(Registration::active)
                    .sorted(FandServiceRegistry::compare)
                    .forEach(registration -> providers.add(registration.provider()));
            return providers;
        }
    }

    @Override
    public <T> Collection<ServiceProvider<T>> providers(Class<T> type) {
        Objects.requireNonNull(type, "type");
        synchronized (lock) {
            return orderedProviders(type);
        }
    }

    @Override
    public <T> Optional<ServiceProvider<T>> provider(Key key, Class<T> type) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(type, "type");
        synchronized (lock) {
            return Optional.ofNullable(registrations.get(new ServiceId(key, type)))
                    .filter(Registration::active)
                    .map(registration -> registration.provider(type));
        }
    }

    @Override
    public <T> ServiceRegistration<T> register(Key key, Class<T> type, T service, ServicePriority priority) {
        return register(key, type, service, priority, "server");
    }

    public <T> ServiceRegistration<T> register(
            Key key,
            Class<T> type,
            T service,
            ServicePriority priority,
            String owner
    ) {
        var provider = new ServiceProvider<>(
                Objects.requireNonNull(key, "key"),
                Objects.requireNonNull(type, "type"),
                Objects.requireNonNull(service, "service"),
                Objects.requireNonNull(owner, "owner"),
                Objects.requireNonNull(priority, "priority"));
        var permissionProvider = permissions != null && type == io.fand.api.permission.PermissionProvider.class
                ? permissions.registerProvider(
                        key,
                        (io.fand.api.permission.PermissionProvider) service,
                        priority,
                        owner)
                : null;
        var registration = new Registration<>(this, provider, sequence.incrementAndGet(), permissionProvider);
        Registration<?> previous;
        synchronized (lock) {
            previous = registrations.put(new ServiceId(key, type), registration);
        }
        if (previous != null) {
            previous.unregisterFromRegistry();
        }
        return registration;
    }

    private <T> Collection<ServiceProvider<T>> orderedProviders(Class<T> type) {
        return registrations.values().stream()
                .filter(Registration::active)
                .filter(registration -> type.isAssignableFrom(registration.type()))
                .sorted(FandServiceRegistry::compare)
                .map(registration -> registration.provider(type))
                .toList();
    }

    private static int compare(Registration<?> left, Registration<?> right) {
        int priority = Integer.compare(right.priority().ordinal(), left.priority().ordinal());
        if (priority != 0) {
            return priority;
        }
        return Long.compare(right.sequence(), left.sequence());
    }

    private void release(Registration<?> registration) {
        synchronized (lock) {
            registrations.remove(new ServiceId(registration.key(), registration.type()), registration);
        }
    }

    @Override
    public void close() {
        Collection<Registration<?>> snapshot;
        synchronized (lock) {
            snapshot = new ArrayList<>(registrations.values());
            registrations.clear();
        }
        snapshot.forEach(Registration::unregisterFromRegistry);
    }

    private record ServiceId(Key key, Class<?> type) {
        private ServiceId {
            Objects.requireNonNull(key, "key");
            Objects.requireNonNull(type, "type");
        }
    }

    private static final class Registration<T> implements ServiceRegistration<T> {

        private final FandServiceRegistry owner;
        private final ServiceProvider<T> provider;
        private final long sequence;
        private final @Nullable ServiceRegistration<?> linkedRegistration;
        private volatile boolean active = true;

        private Registration(
                FandServiceRegistry owner,
                ServiceProvider<T> provider,
                long sequence,
                @Nullable ServiceRegistration<?> linkedRegistration
        ) {
            this.owner = owner;
            this.provider = provider;
            this.sequence = sequence;
            this.linkedRegistration = linkedRegistration;
        }

        @Override
        public Key key() {
            return provider.key();
        }

        @Override
        public Class<T> type() {
            return provider.type();
        }

        @Override
        public T service() {
            return provider.service();
        }

        @Override
        public String owner() {
            return provider.owner();
        }

        @Override
        public ServicePriority priority() {
            return provider.priority();
        }

        @Override
        public boolean active() {
            return active;
        }

        @Override
        public void unregister() {
            if (active) {
                active = false;
                closeLinkedRegistration();
                owner.release(this);
            }
        }

        private long sequence() {
            return sequence;
        }

        private <S> ServiceProvider<S> provider(Class<S> requestedType) {
            return new ServiceProvider<>(
                    provider.key(),
                    requestedType,
                    requestedType.cast(provider.service()),
                    provider.owner(),
                    provider.priority());
        }

        private ServiceProvider<T> provider() {
            return provider;
        }

        private void unregisterFromRegistry() {
            active = false;
            closeLinkedRegistration();
        }

        private void closeLinkedRegistration() {
            if (linkedRegistration != null && linkedRegistration.active()) {
                linkedRegistration.unregister();
            }
        }
    }
}
