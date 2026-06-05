package io.fand.server.permission;

import io.fand.api.permission.PermissionDefault;
import io.fand.api.permission.PermissionDescriptor;
import io.fand.api.permission.PermissionService;
import io.fand.api.permission.PermissionSubject;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

public final class PermissionManager implements PermissionService {

    private static final Pattern NODE = Pattern.compile("[a-z0-9]+(?:[._-][a-z0-9]+)*(?:\\.[a-z0-9]+(?:[._-][a-z0-9]+)*)*");

    private final ConcurrentHashMap<String, PermissionDescriptor> descriptors = new ConcurrentHashMap<>();

    @Override
    public void register(PermissionDescriptor descriptor) {
        Objects.requireNonNull(descriptor, "descriptor");
        var normalized = normalize(descriptor.node());
        var stored = new PermissionDescriptor(normalized, descriptor.defaultAccess());
        var existing = descriptors.putIfAbsent(normalized, stored);
        if (existing != null && existing.defaultAccess() != stored.defaultAccess()) {
            throw new IllegalStateException("Permission already registered with a different default: " + normalized);
        }
    }

    @Override
    public Optional<PermissionDescriptor> lookup(String node) {
        return Optional.ofNullable(descriptors.get(normalize(node)));
    }

    @Override
    public boolean hasPermission(PermissionSubject subject, String node) {
        Objects.requireNonNull(subject, "subject");
        var normalized = normalize(node);

        var explicit = explicitValue(subject, normalized);
        if (explicit != null) {
            return explicit;
        }

        var descriptor = descriptors.get(normalized);
        if (descriptor == null) {
            return false;
        }
        return descriptor.defaultAccess().value(subject.operator());
    }

    private static Boolean explicitValue(PermissionSubject subject, String node) {
        var exact = subject.permissionValue(node).orElse(null);
        if (exact != null) {
            return exact;
        }
        var wildcard = node;
        while (true) {
            var separator = wildcard.lastIndexOf('.');
            if (separator < 0) {
                return subject.permissionValue("*").orElse(null);
            }
            wildcard = wildcard.substring(0, separator);
            var value = subject.permissionValue(wildcard + ".*").orElse(null);
            if (value != null) {
                return value;
            }
        }
    }

    private static String normalize(String node) {
        Objects.requireNonNull(node, "node");
        var normalized = node.trim().toLowerCase(Locale.ROOT);
        if (normalized.equals("*")) {
            return normalized;
        }
        if (!NODE.matcher(normalized).matches()) {
            throw new IllegalArgumentException("Invalid permission node: " + node);
        }
        return normalized;
    }
}
