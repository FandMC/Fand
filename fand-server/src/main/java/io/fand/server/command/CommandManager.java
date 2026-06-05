package io.fand.server.command;

import io.fand.api.command.CommandCompleter;
import io.fand.api.command.CommandDescriptor;
import io.fand.api.command.CommandExecutor;
import io.fand.api.command.CommandRegistration;
import io.fand.api.command.CommandRegistry;
import io.fand.api.command.CommandSender;
import io.fand.api.command.RegisteredCommand;
import io.fand.api.command.ResolvedCommand;
import io.fand.api.permission.PermissionDefault;
import io.fand.api.permission.PermissionDescriptor;
import io.fand.api.permission.PermissionService;
import io.fand.api.permission.PermissionSubject;
import io.fand.server.permission.PermissionManager;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;
import org.jspecify.annotations.Nullable;

public final class CommandManager implements CommandRegistry {

    private static final Pattern NAME = Pattern.compile("[a-z0-9]+(?:[._-][a-z0-9]+)*");

    public CommandManager() {
        this(new PermissionManager());
    }

    public CommandManager(PermissionService permissions) {
        this.permissions = permissions;
    }

    private final Object lock = new Object();
    private final PermissionService permissions;
    private final LinkedHashMap<String, Entry> namespacedPaths = new LinkedHashMap<>();
    private final LinkedHashMap<String, Integer> localRoots = new LinkedHashMap<>();
    private final LinkedHashMap<String, Entry> uniqueLocalRoots = new LinkedHashMap<>();

    @Override
    public CommandRegistration register(Object command) {
        return AnnotatedCommands.register(this, command);
    }

    @Override
    public CommandRegistration register(CommandDescriptor descriptor, CommandExecutor executor, CommandCompleter completer) {
        Objects.requireNonNull(descriptor, "descriptor");
        Objects.requireNonNull(executor, "executor");
        Objects.requireNonNull(completer, "completer");

        var normalized = normalize(descriptor);
        if (normalized.permission() != null && permissions.lookup(normalized.permission()).isEmpty()) {
            permissions.register(new PermissionDescriptor(normalized.permission(), PermissionDefault.OPERATOR));
        }
        var entry = new Entry(normalized, executor, completer, permissions);
        synchronized (lock) {
            for (var key : pathKeys(normalized)) {
                if (namespacedPaths.containsKey(key)) {
                    throw new IllegalStateException("Command path already registered: " + key);
                }
            }
            for (var key : pathKeys(normalized)) {
                namespacedPaths.put(key, entry);
            }
            for (var root : rootKeys(normalized)) {
                trackLocalRoot(root, entry);
            }
        }
        return new Registration(this, entry);
    }

    @Override
    public List<RegisteredCommand> visibleCommands(CommandSender sender) {
        Objects.requireNonNull(sender, "sender");
        synchronized (lock) {
            var seen = new LinkedHashSet<RegisteredCommand>();
            for (var entry : namespacedPaths.values()) {
                if (entry.allowed(sender)) {
                    seen.add(entry);
                }
            }
            return List.copyOf(seen);
        }
    }

    public boolean claims(List<String> tokens) {
        Objects.requireNonNull(tokens, "tokens");
        if (tokens.isEmpty()) {
            return false;
        }
        var normalizedTokens = normalizeTokens(tokens);
        var first = normalizedTokens.getFirst();
        synchronized (lock) {
            if (first.contains(":")) {
                for (var key : namespacedPaths.keySet()) {
                    if (key.startsWith(first)) {
                        return true;
                    }
                }
                return false;
            }
            for (var root : uniqueLocalRoots.keySet()) {
                if (root.startsWith(first) || first.equals(root)) {
                    return true;
                }
            }
            for (var key : namespacedPaths.keySet()) {
                var local = localRoot(key);
                if (local.startsWith(first) || first.equals(local)) {
                    return true;
                }
            }
            return false;
        }
    }

    @Override
    public Optional<RegisteredCommand> lookup(String name) {
        Objects.requireNonNull(name, "name");
        var normalized = normalizeInput(name);
        synchronized (lock) {
            if (normalized.contains(":")) {
                return Optional.ofNullable(namespacedPaths.get(normalized));
            }
            return Optional.ofNullable(uniqueLocalRoots.get(normalized));
        }
    }

    public Optional<ResolvedCommand> resolve(CommandSender sender, List<String> tokens) {
        Objects.requireNonNull(sender, "sender");
        Objects.requireNonNull(tokens, "tokens");
        if (tokens.isEmpty()) {
            return Optional.empty();
        }
        var normalizedTokens = normalizeTokens(tokens);
        synchronized (lock) {
            return normalizedTokens.getFirst().contains(":")
                    ? resolveNamespaced(sender, normalizedTokens)
                    : resolveLocal(sender, normalizedTokens);
        }
    }

    public List<String> suggestions(CommandSender sender, List<String> tokens) {
        Objects.requireNonNull(sender, "sender");
        Objects.requireNonNull(tokens, "tokens");
        synchronized (lock) {
            if (tokens.isEmpty()) {
                return rootSuggestions(sender, "");
            }
            var normalizedTokens = normalizeTokens(tokens);
            if (normalizedTokens.size() == 1) {
                return rootSuggestions(sender, normalizedTokens.getFirst());
            }
            var root = normalizedTokens.getFirst();
            var entry = root.contains(":") ? entryForNamespacedRoot(sender, root) : uniqueLocalRoots.get(root);
            if (entry == null || !entry.allowed(sender)) {
                return List.of();
            }
            return childOrArgumentSuggestions(sender, entry, root, normalizedTokens.subList(1, normalizedTokens.size()));
        }
    }

    private Optional<ResolvedCommand> resolveNamespaced(CommandSender sender, List<String> tokens) {
        var first = tokens.getFirst();
        var separator = first.indexOf(':');
        if (separator <= 0 || separator == first.length() - 1) {
            return Optional.empty();
        }
        var namespace = first.substring(0, separator);
        var root = first.substring(separator + 1);
        return resolvePath(sender, namespace, root, tokens.subList(1, tokens.size()));
    }

    private Optional<ResolvedCommand> resolveLocal(CommandSender sender, List<String> tokens) {
        var rootEntry = uniqueLocalRoots.get(tokens.getFirst());
        if (rootEntry == null || !rootEntry.allowed(sender)) {
            return Optional.empty();
        }
        return resolvePath(sender, rootEntry.descriptor.namespace(), tokens.getFirst(), tokens.subList(1, tokens.size()));
    }

    private Optional<ResolvedCommand> resolvePath(CommandSender sender, String namespace, String root, List<String> tail) {
        for (int length = tail.size(); length >= 0; length--) {
            var key = toPathKey(namespace, root, tail.subList(0, length));
            var entry = namespacedPaths.get(key);
            if (entry != null && entry.allowed(sender)) {
                return Optional.of(new ResolvedCommand(entry, length + 1, root));
            }
        }
        return Optional.empty();
    }

    private @Nullable Entry entryForNamespacedRoot(CommandSender sender, String token) {
        var separator = token.indexOf(':');
        if (separator <= 0 || separator == token.length() - 1) {
            return null;
        }
        var entry = namespacedPaths.get(token);
        return entry != null && entry.allowed(sender) ? entry : null;
    }

    private List<String> rootSuggestions(CommandSender sender, String prefix) {
        var suggestions = new LinkedHashSet<String>();
        for (var entry : uniqueLocalRoots.entrySet()) {
            if (entry.getKey().startsWith(prefix) && entry.getValue().allowed(sender)) {
                suggestions.add(entry.getKey());
            }
        }
        for (var entry : namespacedPaths.values()) {
            if (!entry.allowed(sender)) {
                continue;
            }
            for (var root : rootKeys(entry.descriptor)) {
                var namespaced = entry.descriptor.namespace() + ":" + root;
                if (namespaced.startsWith(prefix)) {
                    suggestions.add(namespaced);
                }
            }
        }
        return List.copyOf(suggestions);
    }

    private List<String> childOrArgumentSuggestions(CommandSender sender, Entry entry, String usedRoot, List<String> subTokens) {
        var path = entry.descriptor.subcommands();
        if (subTokens.size() <= path.size()) {
            for (int i = 0; i < subTokens.size() - 1; i++) {
                if (!path.get(i).equals(subTokens.get(i))) {
                    return List.of();
                }
            }
            var index = subTokens.size() - 1;
            var expected = path.get(index);
            var typed = subTokens.get(index);
            return expected.startsWith(typed) ? List.of(expected) : List.of();
        }
        for (int i = 0; i < path.size(); i++) {
            if (!path.get(i).equals(subTokens.get(i))) {
                return List.of();
            }
        }
        try {
            var args = subTokens.subList(path.size(), subTokens.size());
            return List.copyOf(entry.completer.complete(sender, localRoot(usedRoot), args));
        } catch (Exception ex) {
            throw new IllegalStateException("Command completer failed for " + entry.descriptor.namespace() + ":" + entry.descriptor.label(), ex);
        }
    }

    private void unregister(Entry entry) {
        synchronized (lock) {
            if (!entry.active) {
                return;
            }
            entry.active = false;
            for (var key : pathKeys(entry.descriptor)) {
                namespacedPaths.remove(key);
            }
            for (var root : rootKeys(entry.descriptor)) {
                untrackLocalRoot(root, entry);
            }
        }
    }

    private void trackLocalRoot(String root, Entry entry) {
        var next = localRoots.getOrDefault(root, 0) + 1;
        localRoots.put(root, next);
        if (next == 1) {
            uniqueLocalRoots.put(root, entry);
        } else {
            uniqueLocalRoots.remove(root);
        }
    }

    private void untrackLocalRoot(String root, Entry removed) {
        var current = localRoots.get(root);
        if (current == null) {
            return;
        }
        if (current == 1) {
            localRoots.remove(root);
            uniqueLocalRoots.remove(root);
            return;
        }
        localRoots.put(root, current - 1);
        Entry survivor = null;
        for (var candidate : namespacedPaths.values()) {
            if (candidate == removed) {
                continue;
            }
            if (rootKeys(candidate.descriptor).contains(root)) {
                survivor = candidate;
                break;
            }
        }
        if (current - 1 == 1 && survivor != null) {
            uniqueLocalRoots.put(root, survivor);
        }
    }

    private static List<String> pathKeys(CommandDescriptor descriptor) {
        var keys = new ArrayList<String>(1 + descriptor.aliases().size());
        keys.add(toPathKey(descriptor.namespace(), descriptor.label(), descriptor.subcommands()));
        for (var alias : descriptor.aliases()) {
            keys.add(toPathKey(descriptor.namespace(), alias, descriptor.subcommands()));
        }
        return keys;
    }

    static List<String> rootKeys(CommandDescriptor descriptor) {
        var roots = new ArrayList<String>(1 + descriptor.aliases().size());
        roots.add(descriptor.label());
        roots.addAll(descriptor.aliases());
        return roots;
    }

    private static String toPathKey(String namespace, String root, List<String> subcommands) {
        return subcommands.isEmpty()
                ? namespace + ":" + root
                : namespace + ":" + root + " " + String.join(" ", subcommands);
    }

    private static String localRoot(String usedRoot) {
        var separator = usedRoot.indexOf(':');
        return separator >= 0 ? usedRoot.substring(separator + 1) : usedRoot;
    }

    private static CommandDescriptor normalize(CommandDescriptor descriptor) {
        var namespace = normalizePart(descriptor.namespace(), "namespace");
        var label = normalizePart(descriptor.label(), "label");
        var subcommands = new ArrayList<String>(descriptor.subcommands().size());
        for (var subcommand : descriptor.subcommands()) {
            subcommands.add(normalizePart(subcommand, "subcommand"));
        }
        var aliases = new ArrayList<String>(descriptor.aliases().size());
        for (var alias : descriptor.aliases()) {
            var normalized = normalizePart(alias, "alias");
            if (normalized.equals(label)) {
                continue;
            }
            if (!aliases.contains(normalized)) {
                aliases.add(normalized);
            }
        }
        var permission = descriptor.permission() == null ? null : descriptor.permission().trim();
        return new CommandDescriptor(namespace, label, subcommands, aliases, permission == null || permission.isEmpty() ? null : permission);
    }

    private static List<String> normalizeTokens(List<String> tokens) {
        var normalized = new ArrayList<String>(tokens.size());
        for (var token : tokens) {
            normalized.add(normalizeInput(token));
        }
        return normalized;
    }

    private static String normalizeInput(String value) {
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private static String normalizePart(String value, String role) {
        Objects.requireNonNull(value, role);
        var normalized = normalizeInput(value);
        if (!NAME.matcher(normalized).matches()) {
            throw new IllegalArgumentException("Invalid command " + role + ": " + value);
        }
        return normalized;
    }

    private static final class Registration implements CommandRegistration {

        private final CommandManager owner;
        private final Entry entry;

        private Registration(CommandManager owner, Entry entry) {
            this.owner = owner;
            this.entry = entry;
        }

        @Override
        public boolean active() {
            return entry.active;
        }

        @Override
        public void unregister() {
            owner.unregister(entry);
        }
    }

    public static final class Entry implements RegisteredCommand {

        private final CommandDescriptor descriptor;
        private final CommandExecutor executor;
        private final CommandCompleter completer;
        private final PermissionService permissions;
        private volatile boolean active = true;

        private Entry(CommandDescriptor descriptor, CommandExecutor executor, CommandCompleter completer, PermissionService permissions) {
            this.descriptor = descriptor;
            this.executor = executor;
            this.completer = completer;
            this.permissions = permissions;
        }

        boolean allowed(CommandSender sender) {
            if (descriptor.permission() == null) {
                return true;
            }
            if (sender instanceof PermissionSubject subject) {
                return permissions.hasPermission(subject, descriptor.permission());
            }
            return sender.hasPermission(descriptor.permission());
        }

        @Override
        public CommandDescriptor descriptor() {
            return descriptor;
        }

        @Override
        public CommandExecutor executor() {
            return executor;
        }

        @Override
        public CommandCompleter completer() {
            return completer;
        }
    }
}
