package io.fand.api.permission;

import java.util.Map;
import java.util.Optional;

/**
 * Temporary permission overrides attached to one subject.
 *
 * <p>Attachments are live until {@link #close()} is called. Later attachments
 * take priority over earlier ones, and exact nodes still take priority over
 * wildcard nodes inside the same attachment.
 */
public interface PermissionAttachment extends AutoCloseable {

    PermissionSubject subject();

    boolean active();

    Map<String, Boolean> permissions();

    Optional<Boolean> permissionValue(String node);

    void setPermission(String node, boolean value);

    boolean unsetPermission(String node);

    @Override
    void close();
}
