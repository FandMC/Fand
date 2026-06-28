package io.fand.api.service;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import net.kyori.adventure.key.Key;

public interface ServiceRegistry {

    /**
     * Returns all active providers ordered by {@link ServicePriority} from
     * highest to lowest. Providers with the same priority are ordered by most
     * recent registration first.
     */
    Collection<ServiceProvider<?>> providers();

    /**
     * Returns active providers assignable to {@code type} using the same order
     * as {@link #providers()}. Unregistering the first provider makes the next
     * active provider the fallback for {@link #provider(Class)}.
     */
    <T> Collection<ServiceProvider<T>> providers(Class<T> type);

    /**
     * Returns the first provider from {@link #providers(Class)}.
     */
    default <T> Optional<ServiceProvider<T>> provider(Class<T> type) {
        Objects.requireNonNull(type, "type");
        return providers(type).stream().findFirst();
    }

    <T> Optional<ServiceProvider<T>> provider(Key key, Class<T> type);

    default <T> Optional<T> service(Class<T> type) {
        return provider(type).map(ServiceProvider::service);
    }

    default <T> Optional<T> service(Key key, Class<T> type) {
        return provider(key, type).map(ServiceProvider::service);
    }

    default <T> ServiceRegistration<T> register(Key key, Class<T> type, T service) {
        return register(key, type, service, ServicePriority.NORMAL);
    }

    <T> ServiceRegistration<T> register(Key key, Class<T> type, T service, ServicePriority priority);

    static ServiceRegistry empty() {
        return Empty.INSTANCE;
    }

    enum Empty implements ServiceRegistry {
        INSTANCE;

        @Override
        public Collection<ServiceProvider<?>> providers() {
            return List.of();
        }

        @Override
        public <T> Collection<ServiceProvider<T>> providers(Class<T> type) {
            Objects.requireNonNull(type, "type");
            return List.of();
        }

        @Override
        public <T> Optional<ServiceProvider<T>> provider(Key key, Class<T> type) {
            Objects.requireNonNull(key, "key");
            Objects.requireNonNull(type, "type");
            return Optional.empty();
        }

        @Override
        public <T> ServiceRegistration<T> register(Key key, Class<T> type, T service, ServicePriority priority) {
            throw new UnsupportedOperationException("Service registration is not supported");
        }
    }
}
