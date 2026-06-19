package io.fand.server.plugin;

import io.fand.api.permission.PermissionAttachment;
import io.fand.api.permission.PermissionDescriptor;
import io.fand.api.permission.PermissionService;
import io.fand.api.permission.PermissionSubject;
import java.util.Objects;
import java.util.Optional;

final class PluginPermissionService implements PermissionService {

    private final PermissionService delegate;
    private final PluginResourceTracker tracker;
    private final String pluginId;

    PluginPermissionService(PermissionService delegate, PluginResourceTracker tracker, String pluginId) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
        this.tracker = Objects.requireNonNull(tracker, "tracker");
        this.pluginId = Objects.requireNonNull(pluginId, "pluginId");
    }

    @Override
    public void register(PermissionDescriptor descriptor) {
        PluginRuntime.validatePluginPermissionNode(pluginId, descriptor.node());
        for (var child : descriptor.children().keySet()) {
            PluginRuntime.validatePluginPermissionNode(pluginId, child);
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

    @Override
    public void recalculate(PermissionSubject subject) {
        delegate.recalculate(subject);
    }

    @Override
    public void recalculateAll() {
        delegate.recalculateAll();
    }

    @Override
    public PermissionAttachment attach(PermissionSubject subject) {
        return tracker.track(delegate.attach(subject));
    }
}
