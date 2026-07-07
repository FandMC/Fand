package io.fand.server.permission;

import io.fand.api.event.EventBus;
import io.fand.api.event.permission.PermissionCheckEvent;
import io.fand.api.permission.PermissionAttachment;
import io.fand.api.permission.PermissionContext;
import io.fand.api.permission.PermissionDefault;
import io.fand.api.permission.PermissionDescriptor;
import io.fand.api.permission.PermissionGroup;
import io.fand.api.permission.PermissionMeta;
import io.fand.api.permission.PermissionService;
import io.fand.api.permission.PermissionSubject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
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
    private final ConcurrentHashMap<String, List<PermissionChildParent>> childParents = new ConcurrentHashMap<>();
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
        var normalized = normalizeAttachmentNode(descriptor.node());
        var stored = new PermissionDescriptor(normalized, descriptor.defaultAccess(), normalizeChildren(descriptor.children()));
        synchronized (this) {
            var existing = descriptors.get(normalized);
            var merged = mergeDescriptor(normalized, existing, stored);
            if (existing != null && existing.equals(merged)) {
                return;
            }
            if (existing != null) {
                removeChildIndex(existing);
            }
            descriptors.put(normalized, merged);
            addChildIndex(merged);
        }
    }

    /**
     * Removes the descriptor installed for {@code node}, but only if it still
     * matches {@code expected}. A later re-registration for the same node is
     * never removed by closing an older registration.
     */
    public void unregister(String node, PermissionDescriptor expected) {
        Objects.requireNonNull(node, "node");
        Objects.requireNonNull(expected, "expected");
        var normalized = normalizeAttachmentNode(node);
        var normalizedExpected = new PermissionDescriptor(
                normalized,
                expected.defaultAccess(),
                normalizeChildren(expected.children()));
        synchronized (this) {
            var existing = descriptors.get(normalized);
            if (normalizedExpected.equals(existing)) {
                descriptors.remove(normalized);
                removeChildIndex(existing);
            }
        }
    }

    /**
     * Removes every descriptor whose node lives under one of {@code namespaces}.
     * Used during plugin unload to reclaim plugin-scoped permission declarations
     * and command auto-registered permissions in one pass.
     */
    public void unregisterNamespaces(Set<String> namespaces) {
        Objects.requireNonNull(namespaces, "namespaces");
        if (namespaces.isEmpty()) {
            return;
        }
        var normalized = new HashSet<String>(namespaces.size());
        for (var namespace : namespaces) {
            normalized.add(namespace.trim().toLowerCase(Locale.ROOT));
        }
        var removed = new ArrayList<PermissionDescriptor>();
        synchronized (this) {
            descriptors.forEach((node, descriptor) -> {
                if (underNamespace(node, normalized) && descriptors.remove(node, descriptor)) {
                    removed.add(descriptor);
                }
            });
            removed.forEach(this::removeChildIndex);
        }
    }

    private static boolean underNamespace(String node, Set<String> namespaces) {
        if (namespaces.contains(node)) {
            return true;
        }
        var separator = node.lastIndexOf('.');
        while (separator >= 0) {
            var prefix = node.substring(0, separator);
            if (namespaces.contains(prefix)) {
                return true;
            }
            separator = prefix.lastIndexOf('.');
        }
        return false;
    }

    @Override
    public Optional<PermissionDescriptor> lookup(String node) {
        return Optional.ofNullable(descriptorFor(normalizeAttachmentNode(node)));
    }

    @Override
    public boolean can(PermissionSubject subject, String node) {
        Objects.requireNonNull(subject, "subject");
        var normalized = normalizeAttachmentNode(node);

        return computePermission(subject, normalized);
    }

    @Override
    public PermissionMeta meta(PermissionSubject subject, PermissionContext context) {
        Objects.requireNonNull(subject, "subject");
        Objects.requireNonNull(context, "context");
        return PermissionMeta.empty();
    }

    @Override
    public Optional<PermissionGroup> group(String name, PermissionContext context) {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(context, "context");
        return Optional.empty();
    }

    @Override
    public Collection<PermissionGroup> groups(PermissionContext context) {
        Objects.requireNonNull(context, "context");
        return List.of();
    }

    @Override
    public void recalculate(PermissionSubject subject) {
        Objects.requireNonNull(subject, "subject");
    }

    @Override
    public void recalculateAll() {
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
        var resolved = resolvePermission(subject, normalized, new HashSet<>());
        return fireCheck(subject, normalized, resolved);
    }

    private boolean resolvePermission(PermissionSubject subject, String normalized, HashSet<String> resolving) {
        if (!resolving.add(normalized)) {
            return directDescriptorDefault(subject, normalized);
        }
        var explicit = attachmentValue(subject, normalized);
        if (explicit == null) {
            explicit = explicitValue(subject, normalized);
        }
        if (explicit != null) {
            resolving.remove(normalized);
            return explicit;
        }

        var inherited = descriptorChildValue(subject, normalized, resolving);
        if (inherited != null) {
            resolving.remove(normalized);
            return inherited;
        }

        var result = directDescriptorDefault(subject, normalized);
        resolving.remove(normalized);
        return result;
    }

    private boolean directDescriptorDefault(PermissionSubject subject, String normalized) {
        var descriptor = descriptorFor(normalized);
        if (descriptor == null) {
            return false;
        }
        return descriptor.defaultAccess().value(subject.operator());
    }

    private Boolean descriptorChildValue(PermissionSubject subject, String normalized, HashSet<String> resolving) {
        var parents = childParents.get(normalized);
        if (parents == null || parents.isEmpty()) {
            return null;
        }
        for (var parent : parents) {
            var parentValue = resolvePermission(subject, parent.node(), resolving);
            if (parentValue) {
                return parent.value();
            }
        }
        return null;
    }

    private void addChildIndex(PermissionDescriptor descriptor) {
        for (var child : descriptor.children().entrySet()) {
            childParents.compute(child.getKey(), (node, existing) -> sortedParents(existing, new PermissionChildParent(
                    descriptor.node(),
                    child.getValue())));
        }
    }

    private void removeChildIndex(PermissionDescriptor descriptor) {
        if (descriptor.children().isEmpty()) {
            return;
        }
        for (var child : descriptor.children().keySet()) {
            childParents.computeIfPresent(child, (node, existing) -> {
                var filtered = existing.stream()
                        .filter(parent -> !parent.node().equals(descriptor.node()))
                        .toList();
                return filtered.isEmpty() ? null : filtered;
            });
        }
    }

    private static List<PermissionChildParent> sortedParents(
            @Nullable List<PermissionChildParent> existing,
            PermissionChildParent incoming
    ) {
        var parents = existing == null ? new ArrayList<PermissionChildParent>() : new ArrayList<>(existing);
        parents.add(incoming);
        parents.sort(Comparator.comparingInt((PermissionChildParent parent) -> parent.node().length()).reversed());
        return List.copyOf(parents);
    }

    private @Nullable PermissionDescriptor descriptorFor(String node) {
        var exact = descriptors.get(node);
        if (exact != null) {
            return exact;
        }
        var wildcard = node;
        while (true) {
            var separator = wildcard.lastIndexOf('.');
            if (separator < 0) {
                return descriptors.get("*");
            }
            wildcard = wildcard.substring(0, separator);
            var descriptor = descriptors.get(wildcard + ".*");
            if (descriptor != null) {
                return descriptor;
            }
        }
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

    private static Map<String, Boolean> normalizeChildren(Map<String, Boolean> children) {
        Objects.requireNonNull(children, "children");
        var normalized = new LinkedHashMap<String, Boolean>();
        for (var entry : children.entrySet()) {
            normalized.put(normalizeAttachmentNode(entry.getKey()), Objects.requireNonNull(entry.getValue(), "child permission value"));
        }
        return normalized;
    }

    private static PermissionDescriptor mergeDescriptor(
            String node,
            @Nullable PermissionDescriptor existing,
            PermissionDescriptor incoming
    ) {
        if (existing == null) {
            return incoming;
        }
        if (existing.defaultAccess() != incoming.defaultAccess()) {
            throw new IllegalStateException("Permission already registered with a different default: " + node);
        }
        if (existing.children().equals(incoming.children())) {
            return existing;
        }
        if (existing.children().isEmpty()) {
            return incoming;
        }
        if (incoming.children().isEmpty()) {
            return existing;
        }
        throw new IllegalStateException("Permission already registered with different children: " + node);
    }

    private record PermissionChildParent(String node, boolean value) {
    }
}
