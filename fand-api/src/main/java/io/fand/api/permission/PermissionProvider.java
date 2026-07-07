package io.fand.api.permission;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Supplies ecosystem permission metadata such as chat prefixes, suffixes, and
 * group membership. Permission providers are queried by the server permission
 * service in priority order.
 */
public interface PermissionProvider {

    default PermissionMeta meta(PermissionSubject subject, PermissionContext context) {
        return PermissionMeta.empty();
    }

    default Optional<PermissionGroup> group(String name, PermissionContext context) {
        return Optional.empty();
    }

    default Collection<PermissionGroup> groups(PermissionContext context) {
        return List.of();
    }

    default void recalculate(PermissionSubject subject) {
    }

    default void recalculateAll() {
    }
}
