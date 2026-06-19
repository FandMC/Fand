package io.fand.server.permission;

import io.fand.api.permission.PermissionAttachment;
import io.fand.api.permission.PermissionSubject;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

final class FandPermissionAttachment implements PermissionAttachment {

    private final PermissionManager owner;
    private final PermissionSubject subject;
    private final LinkedHashMap<String, Boolean> permissions = new LinkedHashMap<>();
    private final AtomicBoolean active = new AtomicBoolean(true);

    FandPermissionAttachment(PermissionManager owner, PermissionSubject subject) {
        this.owner = Objects.requireNonNull(owner, "owner");
        this.subject = Objects.requireNonNull(subject, "subject");
    }

    @Override
    public PermissionSubject subject() {
        return subject;
    }

    @Override
    public boolean active() {
        return active.get();
    }

    @Override
    public synchronized Map<String, Boolean> permissions() {
        return Map.copyOf(permissions);
    }

    @Override
    public synchronized Optional<Boolean> permissionValue(String node) {
        if (!active()) {
            return Optional.empty();
        }
        return Optional.ofNullable(permissions.get(PermissionManager.normalizeAttachmentNode(node)));
    }

    @Override
    public synchronized void setPermission(String node, boolean value) {
        ensureActive();
        permissions.put(PermissionManager.normalizeAttachmentNode(node), value);
    }

    @Override
    public synchronized boolean unsetPermission(String node) {
        ensureActive();
        return permissions.remove(PermissionManager.normalizeAttachmentNode(node)) != null;
    }

    @Override
    public void close() {
        if (active.compareAndSet(true, false)) {
            owner.detach(this);
        }
    }

    PermissionSubject attachedSubject() {
        return subject;
    }

    private void ensureActive() {
        if (!active()) {
            throw new IllegalStateException("Permission attachment is closed");
        }
    }
}
