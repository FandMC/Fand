package io.fand.api.permission;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public interface PermissionService {

    void register(PermissionDescriptor descriptor);

    Optional<PermissionDescriptor> lookup(String node);

    boolean can(PermissionSubject subject, String node);

    default boolean allowed(PermissionSubject subject, String node) {
        return can(subject, node);
    }

    default PermissionMeta meta(PermissionSubject subject) {
        return meta(subject, PermissionContext.empty());
    }

    default PermissionMeta meta(PermissionSubject subject, PermissionContext context) {
        Objects.requireNonNull(subject, "subject");
        Objects.requireNonNull(context, "context");
        return PermissionMeta.empty();
    }

    default Optional<String> prefix(PermissionSubject subject) {
        return meta(subject).prefix();
    }

    default Optional<String> prefix(PermissionSubject subject, PermissionContext context) {
        return meta(subject, context).prefix();
    }

    default Optional<String> suffix(PermissionSubject subject) {
        return meta(subject).suffix();
    }

    default Optional<String> suffix(PermissionSubject subject, PermissionContext context) {
        return meta(subject, context).suffix();
    }

    default Optional<String> metaValue(PermissionSubject subject, String key) {
        return meta(subject).value(key);
    }

    default Optional<String> metaValue(PermissionSubject subject, PermissionContext context, String key) {
        return meta(subject, context).value(key);
    }

    default Optional<String> primaryGroup(PermissionSubject subject) {
        return meta(subject).primaryGroup();
    }

    default Optional<String> primaryGroup(PermissionSubject subject, PermissionContext context) {
        return meta(subject, context).primaryGroup();
    }

    default Collection<String> groups(PermissionSubject subject) {
        return meta(subject).groups();
    }

    default Collection<String> groups(PermissionSubject subject, PermissionContext context) {
        return meta(subject, context).groups();
    }

    default Optional<PermissionGroup> group(String name) {
        return group(name, PermissionContext.empty());
    }

    default Optional<PermissionGroup> group(String name, PermissionContext context) {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(context, "context");
        return Optional.empty();
    }

    default Collection<PermissionGroup> groups() {
        return groups(PermissionContext.empty());
    }

    default Collection<PermissionGroup> groups(PermissionContext context) {
        Objects.requireNonNull(context, "context");
        return List.of();
    }

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
