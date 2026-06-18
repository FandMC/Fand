package io.fand.server.plugin;

import io.fand.api.permission.PermissionDescriptor;
import io.fand.api.permission.PermissionService;
import io.fand.api.permission.PermissionSubject;
import java.util.Objects;
import java.util.Optional;

public final class PluginPermissionService implements PermissionService {

    private final PermissionService delegate;
    private final String namespace;

    public PluginPermissionService(PermissionService delegate, String namespace) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
        this.namespace = Objects.requireNonNull(namespace, "namespace");
    }

    @Override
    public void register(PermissionDescriptor descriptor) {
        Objects.requireNonNull(descriptor, "descriptor");
        if (!ownedNode(descriptor.node())) {
            throw new IllegalArgumentException("Plugin " + namespace + " cannot register permission node: " + descriptor.node());
        }
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

    private boolean ownedNode(String node) {
        return node.equals(namespace) || node.startsWith(namespace + ".");
    }
}
