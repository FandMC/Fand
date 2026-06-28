package io.fand.server.plugin;

import io.fand.api.permission.PermissionAttachment;
import io.fand.api.permission.PermissionContext;
import io.fand.api.permission.PermissionDescriptor;
import io.fand.api.permission.PermissionGroup;
import io.fand.api.permission.PermissionMeta;
import io.fand.api.permission.PermissionService;
import io.fand.api.permission.PermissionSubject;
import java.util.Collection;
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
        tracker.trackPermission(descriptor);
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
    public PermissionMeta meta(PermissionSubject subject, PermissionContext context) {
        return delegate.meta(subject, context);
    }

    @Override
    public Optional<PermissionGroup> group(String name, PermissionContext context) {
        return delegate.group(name, context);
    }

    @Override
    public Collection<PermissionGroup> groups(PermissionContext context) {
        return delegate.groups(context);
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
