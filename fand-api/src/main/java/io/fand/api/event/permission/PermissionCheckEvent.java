package io.fand.api.event.permission;

import io.fand.api.event.Event;
import io.fand.api.permission.PermissionSubject;
import java.util.Objects;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/**
 * Fired when the permission service resolves a permission node.
 */
public final class PermissionCheckEvent implements Event {

    private final PermissionSubject subject;
    private final String node;
    private final boolean defaultResult;
    private @Nullable Boolean result;

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
        return Optional.ofNullable(result);
    }

    public void setResult(@Nullable Boolean result) {
        this.result = result;
    }

    public void allow() {
        this.result = true;
    }

    public void deny() {
        this.result = false;
    }

    public boolean effectiveResult() {
        return result == null ? defaultResult : result;
    }
}
