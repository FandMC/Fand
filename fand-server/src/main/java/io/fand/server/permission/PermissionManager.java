package io.fand.server.permission;

import io.fand.api.event.EventBus;
import io.fand.api.event.permission.PermissionCheckEvent;
import io.fand.api.permission.PermissionAttachment;
import io.fand.api.permission.PermissionDefault;
import io.fand.api.permission.PermissionDescriptor;
import io.fand.api.permission.PermissionService;
import io.fand.api.permission.PermissionSubject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class PermissionManager implements PermissionService {

    private static final Logger LOGGER = LoggerFactory.getLogger(PermissionManager.class);
    private static final Pattern NODE = Pattern.compile("[a-z0-9]+(?:[._-][a-z0-9]+)*(?:\\.[a-z0-9]+(?:[._-][a-z0-9]+)*)*");

    private final @Nullable EventBus events;
    private final ConcurrentHashMap<String, PermissionDescriptor> descriptors = new ConcurrentHashMap<>();
    private final Map<PermissionSubject, List<FandPermissionAttachment>> attachments =
            Collections.synchronizedMap(new IdentityHashMap<>());

    public PermissionManager() {
        this(null);
    }

    public PermissionManager(@Nullable EventBus events) {
        this.events = events;
    }

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

        return computePermission(subject, normalized);
    }

    @Override
    public PermissionAttachment attach(PermissionSubject subject) {
        Objects.requireNonNull(subject, "subject");
        var attachment = new FandPermissionAttachment(this, subject);
        synchronized (attachments) {
            attachments.computeIfAbsent(subject, ignored -> new ArrayList<>()).add(attachment);
        }
        return attachment;
    }

    void detach(FandPermissionAttachment attachment) {
        synchronized (attachments) {
            var list = attachments.get(attachment.attachedSubject());
            if (list == null) {
                return;
            }
            list.remove(attachment);
            if (list.isEmpty()) {
                attachments.remove(attachment.attachedSubject());
            }
        }
    }

    private boolean computePermission(PermissionSubject subject, String normalized) {
        var explicit = attachmentValue(subject, normalized);
        if (explicit == null) {
            explicit = explicitValue(subject, normalized);
        }
        if (explicit != null) {
            return fireCheck(subject, normalized, explicit);
        }

        var descriptor = descriptors.get(normalized);
        if (descriptor == null) {
            return fireCheck(subject, normalized, false);
        }
        return fireCheck(subject, normalized, descriptor.defaultAccess().value(subject.operator()));
    }

    private Boolean attachmentValue(PermissionSubject subject, String node) {
        List<FandPermissionAttachment> snapshot;
        synchronized (attachments) {
            var list = attachments.get(subject);
            if (list == null || list.isEmpty()) {
                return null;
            }
            snapshot = List.copyOf(list);
        }
        for (int i = snapshot.size() - 1; i >= 0; i--) {
            var value = explicitAttachmentValue(snapshot.get(i), node);
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private boolean fireCheck(PermissionSubject subject, String node, boolean defaultResult) {
        if (events == null || !events.hasListeners(PermissionCheckEvent.class)) {
            return defaultResult;
        }
        var event = new PermissionCheckEvent(subject, node, defaultResult);
        try {
            events.fire(event);
        } catch (RuntimeException failure) {
            LOGGER.warn("PermissionCheckEvent listener failed", failure);
            return defaultResult;
        }
        return event.effectiveResult();
    }

    private static Boolean explicitAttachmentValue(FandPermissionAttachment attachment, String node) {
        var exact = attachment.permissionValue(node).orElse(null);
        if (exact != null) {
            return exact;
        }
        var wildcard = node;
        while (true) {
            var separator = wildcard.lastIndexOf('.');
            if (separator < 0) {
                return attachment.permissionValue("*").orElse(null);
            }
            wildcard = wildcard.substring(0, separator);
            var value = attachment.permissionValue(wildcard + ".*").orElse(null);
            if (value != null) {
                return value;
            }
        }
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

    static String normalize(String node) {
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

    static String normalizeAttachmentNode(String node) {
        Objects.requireNonNull(node, "node");
        var normalized = node.trim().toLowerCase(Locale.ROOT);
        if (normalized.equals("*")) {
            return normalized;
        }
        if (normalized.endsWith(".*")) {
            var prefix = normalized.substring(0, normalized.length() - 2);
            if (NODE.matcher(prefix).matches()) {
                return normalized;
            }
        } else if (NODE.matcher(normalized).matches()) {
            return normalized;
        }
        throw new IllegalArgumentException("Invalid permission node: " + node);
    }
}
