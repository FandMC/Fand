package io.fand.api.permission;

import java.util.Objects;
import java.util.Optional;

public interface PermissionService {

    void register(PermissionDescriptor descriptor);

    Optional<PermissionDescriptor> lookup(String node);

    boolean hasPermission(PermissionSubject subject, String node);

    default void recalculate(PermissionSubject subject) {
        Objects.requireNonNull(subject, "subject");
    }

    default void recalculateAll() {
    }

    default PermissionAttachment attach(PermissionSubject subject) {
        throw new UnsupportedOperationException("Permission attachments are not supported");
    }

    default PermissionAttachment attach(PermissionSubject subject, String node, boolean value) {
        var attachment = attach(subject);
        try {
            attachment.setPermission(node, value);
        } catch (RuntimeException | Error failure) {
            attachment.close();
            throw failure;
        }
        return attachment;
    }
}
