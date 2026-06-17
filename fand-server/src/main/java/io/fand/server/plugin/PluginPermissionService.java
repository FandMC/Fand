package io.fand.server.plugin;

import io.fand.api.permission.PermissionDescriptor;
import io.fand.api.permission.PermissionService;
import io.fand.api.permission.PermissionSubject;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

final class PluginPermissionService implements PermissionService {

    private final PermissionService delegate;
    private final String namespace;
    private final String prefix;

    PluginPermissionService(PermissionService delegate, String namespace) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
        this.namespace = Objects.requireNonNull(namespace, "namespace").toLowerCase(Locale.ROOT);
        this.prefix = this.namespace + ".";
    }

    @Override
    public void register(PermissionDescriptor descriptor) {
        Objects.requireNonNull(descriptor, "descriptor");
        requireOwned(descriptor.node());
        delegate.register(descriptor);
    }

    @Override
    public Optional<PermissionDescriptor> lookup(String node) {
        return delegate.lookup(node);
    }

    @Override
    public boolean hasPermission(PermissionSubject subject, String node) {
        return delegate.hasPermission(subject, node);
    }

    void requireOwned(String permission) {
        var normalized = permission.trim().toLowerCase(Locale.ROOT);
        if (!normalized.equals(namespace) && !normalized.startsWith(prefix)) {
            throw new IllegalArgumentException("Plugin " + namespace + " cannot register permission outside its namespace: " + permission);
        }
    }
}
