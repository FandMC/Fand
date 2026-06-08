package io.fand.api.event.permission;

import io.fand.api.event.Event;
import io.fand.api.permission.PermissionSubject;
import java.util.Objects;
import java.util.Optional;

/**
 * Fired when the permission service resolves a permission node.
 */
public final class PermissionCheckEvent implements Event {

    private final PermissionSubject subject;
    private final String node;
    private final boolean defaultResult;
    private Optional<Boolean> result = Optional.empty();

    public PermissionCheckEvent(PermissionSubject subject, String node, boolean defaultResult) {
        this.subject = Objects.requireNonNull(subject, "subject");
        this.node = Objects.requireNonNull(node, "node");
        this.defaultResult = defaultResult;
    }

    public PermissionSubject subject() {
        return subject;
    }

    public String node() {
        return node;
    }

    public boolean defaultResult() {
        return defaultResult;
    }

    public Optional<Boolean> result() {
        return result;
    }

    public void setResult(Optional<Boolean> result) {
        this.result = Objects.requireNonNull(result, "result");
    }

    public void allow() {
        this.result = Optional.of(true);
    }

    public void deny() {
        this.result = Optional.of(false);
    }

    public boolean effectiveResult() {
        return result.orElse(defaultResult);
    }
}
