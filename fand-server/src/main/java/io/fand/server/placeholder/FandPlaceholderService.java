package io.fand.server.placeholder;

import io.fand.api.entity.Player;
import io.fand.api.placeholder.PlaceholderProvider;
import io.fand.api.placeholder.PlaceholderRegistration;
import io.fand.api.placeholder.PlaceholderService;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.jspecify.annotations.Nullable;

public final class FandPlaceholderService implements PlaceholderService, AutoCloseable {

    private final ConcurrentHashMap<String, Registration> providers = new ConcurrentHashMap<>();

    @Override
    public PlaceholderRegistration register(String namespace, PlaceholderProvider provider) {
        var normalized = normalizeNamespace(namespace);
        var registration = new Registration(normalized, Objects.requireNonNull(provider, "provider"), this);
        var previous = providers.put(normalized, registration);
        if (previous != null) {
            previous.unregisterFromService();
        }
        return registration;
    }

    @Override
    public Optional<String> resolve(@Nullable Player viewer, String identifier) {
        var normalized = normalizeIdentifier(identifier);
        var provider = provider(normalized).orElse(null);
        if (provider == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(provider.resolve(viewer, normalized));
    }

    @Override
    public void close() {
        var snapshot = java.util.List.copyOf(providers.values());
        providers.clear();
        snapshot.forEach(Registration::unregisterFromService);
    }

    void remove(Registration registration) {
        providers.remove(registration.namespace(), registration);
    }

    private Optional<PlaceholderProvider> provider(String identifier) {
        var direct = providers.get(identifier);
        if (direct != null && direct.active()) {
            return Optional.of(direct.provider());
        }
        var split = identifier.indexOf('_');
        if (split <= 0) {
            return Optional.empty();
        }
        var namespace = identifier.substring(0, split);
        var namespaced = providers.get(namespace);
        return namespaced != null && namespaced.active()
                ? Optional.of(namespaced.provider())
                : Optional.empty();
    }

    private static String normalizeNamespace(String namespace) {
        var normalized = normalizeIdentifier(namespace);
        if (normalized.indexOf('_') >= 0) {
            throw new IllegalArgumentException("Placeholder namespace cannot contain '_': " + namespace);
        }
        return normalized;
    }

    private static String normalizeIdentifier(String identifier) {
        Objects.requireNonNull(identifier, "identifier");
        var normalized = identifier.trim().toLowerCase(Locale.ROOT);
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("Placeholder identifier cannot be empty");
        }
        for (int index = 0; index < normalized.length(); index++) {
            var ch = normalized.charAt(index);
            if (!(ch >= 'a' && ch <= 'z') && !(ch >= '0' && ch <= '9') && ch != '_' && ch != '-' && ch != '.') {
                throw new IllegalArgumentException("Invalid placeholder identifier: " + identifier);
            }
        }
        return normalized;
    }

    private static final class Registration implements PlaceholderRegistration {

        private final String namespace;
        private final PlaceholderProvider provider;
        private final FandPlaceholderService owner;
        private volatile boolean active = true;

        private Registration(String namespace, PlaceholderProvider provider, FandPlaceholderService owner) {
            this.namespace = namespace;
            this.provider = provider;
            this.owner = owner;
        }

        @Override
        public String namespace() {
            return namespace;
        }

        private PlaceholderProvider provider() {
            return provider;
        }

        @Override
        public boolean active() {
            return active;
        }

        @Override
        public void unregister() {
            if (active) {
                active = false;
                owner.remove(this);
            }
        }

        private void unregisterFromService() {
            active = false;
        }
    }
}
