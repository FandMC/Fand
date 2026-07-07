package io.fand.server.command;

import io.fand.api.command.Argument;
import io.fand.api.command.CommandArgument;
import io.fand.api.command.CommandArgumentType;
import io.fand.api.command.CommandContext;
import io.fand.api.command.CommandDefinition;
import io.fand.api.command.CommandInfo;
import io.fand.api.command.CommandNode;
import io.fand.api.command.CommandNodeType;
import io.fand.api.command.CommandRegistration;
import io.fand.api.command.CommandRegistry;
import io.fand.api.command.CommandSender;
import io.fand.api.command.CommandSuggestionProvider;
import io.fand.api.entity.Player;
import io.fand.api.permission.PermissionDefault;
import io.fand.api.permission.PermissionDescriptor;
import io.fand.api.permission.PermissionService;
import io.fand.api.permission.PermissionSubject;
import io.fand.server.permission.PermissionManager;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;
import org.jspecify.annotations.Nullable;

public final class CommandManager implements CommandRegistry {

    private static final Pattern NAME = Pattern.compile("[a-z0-9]+(?:[._-][a-z0-9]+)*");

    private final Object lock = new Object();
    private final PermissionService permissions;
    private volatile Snapshot snapshot = Snapshot.empty();

    public CommandManager() {
        this(new PermissionManager());
    }

    public CommandManager(PermissionService permissions) {
        this.permissions = permissions;
    }

    @Override
    public CommandRegistration register(Object command) {
        return AnnotatedCommands.register(this, command);
    }

    @Override
    public CommandRegistration register(CommandDefinition definition) {
        Objects.requireNonNull(definition, "definition");
        var pending = flatten(definition);
        if (pending.isEmpty()) {
            throw new IllegalArgumentException("Command has no executable nodes: " + definition.label());
        }
        return registerAll(pending);
    }

    @Override
    public List<CommandInfo> visibleCommands(CommandSender sender) {
        Objects.requireNonNull(sender, "sender");
        var current = snapshot;
        var seen = new LinkedHashSet<CommandInfo>();
        for (var entries : current.namespacedPaths.values()) {
            for (var entry : entries) {
                if (entry.allowed(sender)) {
                    seen.add(entry.info());
                }
            }
        }
        return List.copyOf(seen);
    }

    @Override
    public boolean claims(List<String> tokens) {
        Objects.requireNonNull(tokens, "tokens");
        if (tokens.isEmpty()) {
            return false;
        }
        var normalizedTokens = normalizeTokens(tokens);
        var first = normalizedTokens.getFirst();
        var current = snapshot;
        if (first.contains(":")) {
            return claimsNamespacedRoot(current, first);
        }
        return current.uniqueLocalRoots.containsKey(first);
    }

    @Override
    public Optional<CommandInfo> lookup(String name) {
        Objects.requireNonNull(name, "name");
        var normalized = normalizeInput(name);
        var current = snapshot;
        if (normalized.contains(":")) {
            return firstActive(rootEntries(current, normalized)).map(CommandEntry::info);
        }
        var owner = current.uniqueLocalRoots.get(normalized);
        return owner == null ? Optional.empty() : firstActive(rootEntries(current, owner)).map(CommandEntry::info);
    }

    public Optional<ResolvedCommand> resolve(CommandSender sender, List<String> tokens) {
        Objects.requireNonNull(sender, "sender");
        Objects.requireNonNull(tokens, "tokens");
        if (tokens.isEmpty()) {
            return Optional.empty();
        }
        var normalizedTokens = normalizeTokens(tokens);
        var current = snapshot;
        return normalizedTokens.getFirst().contains(":")
                ? resolveNamespaced(current, sender, normalizedTokens)
                : resolveLocal(current, sender, normalizedTokens);
    }

    @Override
    public List<String> suggestions(CommandSender sender, List<String> tokens) {
        Objects.requireNonNull(sender, "sender");
        Objects.requireNonNull(tokens, "tokens");
        var current = snapshot;
        if (tokens.isEmpty()) {
            return rootSuggestions(current, sender, "");
        }
        var normalizedTokens = normalizeTokens(tokens);
        if (normalizedTokens.size() == 1) {
            return rootSuggestions(current, sender, normalizedTokens.getFirst());
        }
        var root = normalizedTokens.getFirst();
        var entries = root.contains(":")
                ? rootEntries(current, root)
                : current.uniqueLocalRoots.containsKey(root) ? rootEntries(current, current.uniqueLocalRoots.get(root)) : List.<CommandEntry>of();
        if (entries.isEmpty()) {
            return List.of();
        }
        return childOrArgumentSuggestions(sender, entries, root, normalizedTokens.subList(1, normalizedTokens.size()));
    }

    private CommandRegistration registerAll(List<PendingEntry> pendingEntries) {
        var entries = new ArrayList<CommandEntry>(pendingEntries.size());
        synchronized (lock) {
            var current = snapshot;
            for (var pending : pendingEntries) {
                validateAvailable(current, pending.info());
            }

            var namespacedPaths = copyPaths(current.namespacedPaths);
            var localRoots = copyLocalRoots(current.localRoots);
            var uniqueLocalRoots = new LinkedHashMap<>(current.uniqueLocalRoots);
            for (var pending : pendingEntries) {
                var info = pending.info();
                PermissionDescriptor autoPermission = null;
                if (info.permission() != null && permissions.lookup(info.permission()).isEmpty()) {
                    autoPermission = new PermissionDescriptor(info.permission(), PermissionDefault.OPERATOR);
                    permissions.register(autoPermission);
                }
                var entry = new CommandEntry(info, pending.executor(), pending.completer(), permissions, autoPermission, pending.strictArguments());
                entries.add(entry);
                for (var key : pathKeys(info)) {
                    namespacedPaths.computeIfAbsent(key, ignored -> new ArrayList<>()).add(entry);
                }
                for (var root : rootKeys(info)) {
                    trackLocalRoot(root, rootOwner(info.namespace(), root), localRoots, uniqueLocalRoots);
                }
            }
            snapshot = Snapshot.of(namespacedPaths, localRoots, uniqueLocalRoots);
        }
        return entries.size() == 1 ? new Registration(this, entries.getFirst()) : new CombinedRegistration(this, entries);
    }

    private static void validateAvailable(Snapshot current, CommandInfo info) {
        for (var key : pathKeys(info)) {
            var existing = current.namespacedPaths.getOrDefault(key, List.of());
            for (var entry : existing) {
                if (sameArgumentSignature(entry.info, info)) {
                    throw new IllegalStateException("Command path already registered: " + key);
                }
            }
        }
    }

    private Optional<ResolvedCommand> resolveNamespaced(Snapshot current, CommandSender sender, List<String> tokens) {
        var first = tokens.getFirst();
        var separator = first.indexOf(':');
        if (separator <= 0 || separator == first.length() - 1) {
            return Optional.empty();
        }
        var namespace = first.substring(0, separator);
        var root = first.substring(separator + 1);
        return resolvePath(current, sender, namespace, root, tokens.subList(1, tokens.size()));
    }

    private Optional<ResolvedCommand> resolveLocal(Snapshot current, CommandSender sender, List<String> tokens) {
        var owner = current.uniqueLocalRoots.get(tokens.getFirst());
        if (owner == null) {
            return Optional.empty();
        }
        var separator = owner.indexOf(':');
        return resolvePath(current, sender, owner.substring(0, separator), tokens.getFirst(), tokens.subList(1, tokens.size()));
    }

    private Optional<ResolvedCommand> resolvePath(Snapshot current, CommandSender sender, String namespace, String root, List<String> tail) {
        for (int length = tail.size(); length >= 0; length--) {
            var key = toPathKey(namespace, root, tail.subList(0, length));
            var entries = current.namespacedPaths.get(key);
            if (entries == null) {
                continue;
            }
            var args = tail.subList(length, tail.size());
            for (var entry : entries) {
                if (entry.allowed(sender) && entry.matchesArguments(args)) {
                    return Optional.of(new ResolvedCommand(entry, length + 1, root));
                }
            }
        }
        return Optional.empty();
    }

    private List<String> rootSuggestions(Snapshot current, CommandSender sender, String prefix) {
        var suggestions = new LinkedHashSet<String>();
        for (var entry : current.uniqueLocalRoots.entrySet()) {
            if (entry.getKey().startsWith(prefix) && ownerAllowed(current, sender, entry.getValue())) {
                suggestions.add(entry.getKey());
            }
        }
        for (var roots : current.localRoots.entrySet()) {
            for (var owner : roots.getValue().keySet()) {
                if (owner.startsWith(prefix) && ownerAllowed(current, sender, owner)) {
                    suggestions.add(owner);
                }
            }
        }
        return List.copyOf(suggestions);
    }

    private boolean ownerAllowed(Snapshot current, CommandSender sender, String owner) {
        for (var entry : rootEntries(current, owner)) {
            if (entry.allowed(sender)) {
                return true;
            }
        }
        return false;
    }

    private List<String> childOrArgumentSuggestions(CommandSender sender, List<CommandEntry> entries, String usedRoot, List<String> subTokens) {
        var suggestions = new LinkedHashSet<String>();
        for (var entry : entries) {
            if (!entry.allowed(sender)) {
                continue;
            }
            addSuggestions(sender, entry, usedRoot, subTokens, suggestions);
        }
        return List.copyOf(suggestions);
    }

    private static void addSuggestions(
            CommandSender sender,
            CommandEntry entry,
            String usedRoot,
            List<String> subTokens,
            LinkedHashSet<String> suggestions
    ) {
        var path = entry.info.path();
        if (subTokens.size() <= path.size()) {
            for (int i = 0; i < subTokens.size() - 1; i++) {
                if (!path.get(i).equals(subTokens.get(i))) {
                    return;
                }
            }
            var index = subTokens.size() - 1;
            var expected = path.get(index);
            var typed = subTokens.get(index);
            if (expected.startsWith(typed)) {
                suggestions.add(expected);
            }
            return;
        }
        for (int i = 0; i < path.size(); i++) {
            if (!path.get(i).equals(subTokens.get(i))) {
                return;
            }
        }
        try {
            var args = subTokens.subList(path.size(), subTokens.size());
            suggestions.addAll(entry.complete(sender, localRoot(usedRoot), args));
        } catch (Exception ex) {
            throw new IllegalStateException("Command completer failed for " + entry.info.namespace() + ":" + entry.info.label(), ex);
        }
    }

    private void unregister(CommandEntry entry) {
        synchronized (lock) {
            if (!entry.active) {
                return;
            }
            var current = snapshot;
            var namespacedPaths = copyPaths(current.namespacedPaths);
            var localRoots = copyLocalRoots(current.localRoots);
            var uniqueLocalRoots = new LinkedHashMap<>(current.uniqueLocalRoots);
            entry.active = false;
            for (var key : pathKeys(entry.info)) {
                removePathEntry(namespacedPaths, key, entry);
            }
            for (var root : rootKeys(entry.info)) {
                untrackLocalRoot(root, rootOwner(entry.info.namespace(), root), localRoots, uniqueLocalRoots);
            }
            snapshot = Snapshot.of(namespacedPaths, localRoots, uniqueLocalRoots);
        }
        entry.relinquishAutoPermission();
    }

    private void unregisterAll(List<CommandEntry> entries) {
        for (var entry : entries) {
            unregister(entry);
        }
    }

    private static void removePathEntry(LinkedHashMap<String, List<CommandEntry>> namespacedPaths, String key, CommandEntry removed) {
        var entries = namespacedPaths.get(key);
        if (entries == null) {
            return;
        }
        entries.remove(removed);
        if (entries.isEmpty()) {
            namespacedPaths.remove(key);
        }
    }

    private static void trackLocalRoot(
            String root,
            String owner,
            LinkedHashMap<String, LinkedHashMap<String, Integer>> localRoots,
            LinkedHashMap<String, String> uniqueLocalRoots
    ) {
        var owners = localRoots.computeIfAbsent(root, ignored -> new LinkedHashMap<>());
        owners.put(owner, owners.getOrDefault(owner, 0) + 1);
        if (owners.size() == 1) {
            uniqueLocalRoots.put(root, owner);
        } else {
            uniqueLocalRoots.remove(root);
        }
    }

    private static void untrackLocalRoot(
            String root,
            String owner,
            LinkedHashMap<String, LinkedHashMap<String, Integer>> localRoots,
            LinkedHashMap<String, String> uniqueLocalRoots
    ) {
        var owners = localRoots.get(root);
        if (owners == null) {
            return;
        }
        var count = owners.get(owner);
        if (count == null) {
            return;
        }
        if (count <= 1) {
            owners.remove(owner);
        } else {
            owners.put(owner, count - 1);
        }
        if (owners.isEmpty()) {
            localRoots.remove(root);
            uniqueLocalRoots.remove(root);
        } else if (owners.size() == 1) {
            uniqueLocalRoots.put(root, owners.keySet().iterator().next());
        } else {
            uniqueLocalRoots.remove(root);
        }
    }

    private static List<PendingEntry> flatten(CommandDefinition definition) {
        var namespace = normalizePart(definition.namespace(), "namespace");
        var label = normalizePart(definition.label(), "label");
        var aliases = new ArrayList<String>(definition.aliases().size());
        for (var alias : definition.aliases()) {
            var normalized = normalizePart(alias, "alias");
            if (!normalized.equals(label) && !aliases.contains(normalized)) {
                aliases.add(normalized);
            }
        }
        var permission = normalizePermission(definition.permission());
        var pending = new ArrayList<PendingEntry>();
        flattenNode(
                definition.root(),
                namespace,
                label,
                aliases,
                permission,
                new ArrayList<>(),
                new ArrayList<>(),
                pending);
        return pending;
    }

    private static void flattenNode(
            CommandNode node,
            String namespace,
            String label,
            List<String> aliases,
            @Nullable String inheritedPermission,
            ArrayList<String> path,
            ArrayList<RuntimeArgument> arguments,
            ArrayList<PendingEntry> pending
    ) {
        var nodePermission = normalizePermission(node.permission());
        var permission = nodePermission == null ? inheritedPermission : nodePermission;
        if (node.action() != null) {
            var runtimeArguments = List.copyOf(arguments);
            var info = info(namespace, label, aliases, permission, path, runtimeArguments);
            pending.add(new PendingEntry(
                    info,
                    (sender, usedLabel, args) -> node.action().execute(context(sender, usedLabel, args, runtimeArguments)),
                    completer(runtimeArguments, node.suggestions()),
                    true));
        }
        for (var child : node.children()) {
            if (child.type() == CommandNodeType.LITERAL) {
                if (!arguments.isEmpty()) {
                    throw new IllegalArgumentException("Literal children after arguments are not supported: " + child.name());
                }
                path.add(normalizePart(child.name(), "subcommand"));
                flattenNode(child, namespace, label, aliases, permission, path, arguments, pending);
                path.removeLast();
            } else if (child.type() == CommandNodeType.ARGUMENT) {
                arguments.add(new RuntimeArgument(normalizePart(child.name(), "argument"), child.argument()));
                flattenNode(child, namespace, label, aliases, permission, path, arguments, pending);
                arguments.removeLast();
            } else {
                flattenNode(child, namespace, label, aliases, permission, path, arguments, pending);
            }
        }
    }

    private static CommandInfo info(
            String namespace,
            String label,
            List<String> aliases,
            @Nullable String permission,
            List<String> path,
            List<RuntimeArgument> arguments
    ) {
        var typed = arguments.stream()
                .map(argument -> argument.argument().metadata(argument.name()))
                .toList();
        return new CommandInfo(namespace, label, path, typed, aliases, permission);
    }

    private static RuntimeCommandCompleter completer(List<RuntimeArgument> arguments, @Nullable CommandSuggestionProvider suggestions) {
        var runtimeArguments = List.copyOf(arguments);
        return (sender, label, args) -> {
            if (suggestions != null) {
                var context = partialContext(sender, label, args, runtimeArguments);
                return filter(suggestions.suggest(context), args.isEmpty() ? "" : args.getLast());
            }
            return argumentSuggestions(runtimeArguments, suggestionArgumentIndex(args), args.isEmpty() ? "" : args.getLast());
        };
    }

    private static int suggestionArgumentIndex(List<String> args) {
        if (args.isEmpty()) {
            return 0;
        }
        return Math.max(0, args.size() - 1);
    }

    private static List<String> argumentSuggestions(List<RuntimeArgument> arguments, int index, String prefix) {
        if (index < 0 || index >= arguments.size()) {
            return List.of();
        }
        return filter(arguments.get(index).argument().suggestions(), prefix);
    }

    private static List<String> filter(List<String> values, String prefix) {
        var normalized = prefix.toLowerCase(Locale.ROOT);
        return values.stream()
                .filter(value -> value.toLowerCase(Locale.ROOT).startsWith(normalized))
                .toList();
    }

    private static CommandContext context(CommandSender sender, String label, List<String> args, List<RuntimeArgument> runtimeArguments) throws Exception {
        var values = new LinkedHashMap<String, Object>();
        var index = 0;
        for (var runtimeArgument : runtimeArguments) {
            var argument = runtimeArgument.argument();
            if (index >= args.size()) {
                addMissingArgument(sender, values, runtimeArgument);
                continue;
            }
            var raw = argument.type() == CommandArgumentType.GREEDY_STRING
                    ? String.join(" ", args.subList(index, args.size()))
                    : args.get(index);
            index = argument.type() == CommandArgumentType.GREEDY_STRING ? args.size() : index + 1;
            values.put(runtimeArgument.name(), argument.parse(raw));
        }
        if (index < args.size()) {
            throw new IllegalArgumentException("Too many command arguments: " + String.join(" ", args.subList(index, args.size())));
        }
        return new CommandContext(sender, label, args, values);
    }

    private static CommandContext partialContext(CommandSender sender, String label, List<String> args, List<RuntimeArgument> runtimeArguments) throws Exception {
        var completed = args.isEmpty() ? List.<String>of() : args.subList(0, args.size() - 1);
        var usableArguments = runtimeArguments.subList(0, Math.min(completed.size(), runtimeArguments.size()));
        return context(sender, label, completed, usableArguments);
    }

    private static void addMissingArgument(CommandSender sender, LinkedHashMap<String, Object> values, RuntimeArgument runtimeArgument) {
        var argument = runtimeArgument.argument();
        if (argument.usesSenderAsDefault() && argument.valueType().isInstance(sender)) {
            values.put(runtimeArgument.name(), argument.valueType().cast(sender));
            return;
        }
        if (argument.defaultValue() != null) {
            values.put(runtimeArgument.name(), argument.defaultValue());
            return;
        }
        if (argument.optional()) {
            return;
        }
        throw new IllegalArgumentException("Missing command argument: " + runtimeArgument.name());
    }

    private static Optional<CommandEntry> firstActive(List<CommandEntry> entries) {
        CommandEntry fallback = null;
        for (var entry : entries) {
            if (!entry.active()) {
                continue;
            }
            if (fallback == null) {
                fallback = entry;
            }
            if (entry.info.path().isEmpty()) {
                return Optional.of(entry);
            }
        }
        return Optional.ofNullable(fallback);
    }

    private static List<CommandEntry> rootEntries(Snapshot current, String namespacedRoot) {
        var entries = new LinkedHashSet<CommandEntry>();
        for (var path : current.namespacedPaths.entrySet()) {
            if (namespacedRoot.equals(pathRoot(path.getKey()))) {
                entries.addAll(path.getValue());
            }
        }
        return List.copyOf(entries);
    }

    private boolean claimsNamespacedRoot(Snapshot current, String root) {
        for (var key : current.namespacedPaths.keySet()) {
            if (root.equals(pathRoot(key))) {
                return true;
            }
        }
        return false;
    }

    private record Snapshot(
            Map<String, List<CommandEntry>> namespacedPaths,
            Map<String, Map<String, Integer>> localRoots,
            Map<String, String> uniqueLocalRoots
    ) {

        private static Snapshot empty() {
            return of(new LinkedHashMap<>(), new LinkedHashMap<>(), new LinkedHashMap<>());
        }

        private static Snapshot of(
                LinkedHashMap<String, List<CommandEntry>> namespacedPaths,
                LinkedHashMap<String, LinkedHashMap<String, Integer>> localRoots,
                LinkedHashMap<String, String> uniqueLocalRoots
        ) {
            var paths = new LinkedHashMap<String, List<CommandEntry>>();
            namespacedPaths.forEach((key, entries) -> paths.put(key, List.copyOf(entries)));
            var roots = new LinkedHashMap<String, Map<String, Integer>>();
            localRoots.forEach((key, owners) -> roots.put(key, Collections.unmodifiableMap(new LinkedHashMap<>(owners))));
            return new Snapshot(
                    Collections.unmodifiableMap(paths),
                    Collections.unmodifiableMap(roots),
                    Collections.unmodifiableMap(new LinkedHashMap<>(uniqueLocalRoots))
            );
        }
    }

    private static LinkedHashMap<String, List<CommandEntry>> copyPaths(Map<String, List<CommandEntry>> paths) {
        var copy = new LinkedHashMap<String, List<CommandEntry>>();
        paths.forEach((key, entries) -> copy.put(key, new ArrayList<>(entries)));
        return copy;
    }

    private static LinkedHashMap<String, LinkedHashMap<String, Integer>> copyLocalRoots(Map<String, Map<String, Integer>> roots) {
        var copy = new LinkedHashMap<String, LinkedHashMap<String, Integer>>();
        roots.forEach((key, owners) -> copy.put(key, new LinkedHashMap<>(owners)));
        return copy;
    }

    static List<String> rootKeys(CommandInfo info) {
        var roots = new ArrayList<String>(1 + info.aliases().size());
        roots.add(info.label());
        roots.addAll(info.aliases());
        return roots;
    }

    private static List<String> pathKeys(CommandInfo info) {
        var keys = new ArrayList<String>(1 + info.aliases().size());
        keys.add(toPathKey(info.namespace(), info.label(), info.path()));
        for (var alias : info.aliases()) {
            keys.add(toPathKey(info.namespace(), alias, info.path()));
        }
        return keys;
    }

    private static String toPathKey(String namespace, String root, List<String> path) {
        return path.isEmpty()
                ? namespace + ":" + root
                : namespace + ":" + root + " " + String.join(" ", path);
    }

    private static String rootOwner(String namespace, String root) {
        return namespace + ":" + root;
    }

    private static String localRoot(String usedRoot) {
        var separator = usedRoot.indexOf(':');
        return separator >= 0 ? usedRoot.substring(separator + 1) : usedRoot;
    }

    private static String pathRoot(String pathKey) {
        var separator = pathKey.indexOf(' ');
        return separator < 0 ? pathKey : pathKey.substring(0, separator);
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

    private static @Nullable String normalizePermission(@Nullable String permission) {
        if (permission == null) {
            return null;
        }
        var normalized = permission.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private static boolean sameArgumentSignature(CommandInfo first, CommandInfo second) {
        return first.arguments().stream().map(CommandArgument::name).toList()
                .equals(second.arguments().stream().map(CommandArgument::name).toList());
    }

    private record PendingEntry(
            CommandInfo info,
            RuntimeCommandExecutor executor,
            RuntimeCommandCompleter completer,
            boolean strictArguments
    ) {
    }

    private record RuntimeArgument(String name, Argument<?> argument) {
        private RuntimeArgument {
            Objects.requireNonNull(name, "name");
            Objects.requireNonNull(argument, "argument");
        }
    }

    @FunctionalInterface
    private interface RuntimeCommandExecutor {
        void execute(CommandSender sender, String label, List<String> args) throws Exception;
    }

    @FunctionalInterface
    private interface RuntimeCommandCompleter {
        List<String> complete(CommandSender sender, String label, List<String> args) throws Exception;
    }

    private static final class Registration implements CommandRegistration {

        private final CommandManager owner;
        private final CommandEntry entry;

        private Registration(CommandManager owner, CommandEntry entry) {
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

    private static final class CombinedRegistration implements CommandRegistration {

        private final CommandManager owner;
        private final List<CommandEntry> entries;

        private CombinedRegistration(CommandManager owner, List<CommandEntry> entries) {
            this.owner = owner;
            this.entries = List.copyOf(entries);
        }

        @Override
        public boolean active() {
            return entries.stream().anyMatch(CommandEntry::active);
        }

        @Override
        public void unregister() {
            owner.unregisterAll(entries);
        }
    }

    public record ResolvedCommand(CommandEntry command, int matchedLength, String usedLabel) {
    }

    public static final class CommandEntry {

        private final CommandInfo info;
        private final RuntimeCommandExecutor executor;
        private final RuntimeCommandCompleter completer;
        private final PermissionService permissions;
        private final @Nullable PermissionDescriptor autoPermission;
        private final boolean strictArguments;
        private volatile boolean active = true;

        private CommandEntry(
                CommandInfo info,
                RuntimeCommandExecutor executor,
                RuntimeCommandCompleter completer,
                PermissionService permissions,
                @Nullable PermissionDescriptor autoPermission,
                boolean strictArguments
        ) {
            this.info = info;
            this.executor = executor;
            this.completer = completer;
            this.permissions = permissions;
            this.autoPermission = autoPermission;
            this.strictArguments = strictArguments;
        }

        public CommandInfo info() {
            return info;
        }

        public void execute(CommandSender sender, String label, List<String> args) throws Exception {
            executor.execute(sender, label, args);
        }

        public List<String> complete(CommandSender sender, String label, List<String> args) throws Exception {
            return completer.complete(sender, label, args);
        }

        private void relinquishAutoPermission() {
            var registered = autoPermission;
            if (registered == null) {
                return;
            }
            if (permissions instanceof PermissionManager manager) {
                manager.unregister(registered.node(), registered);
            }
        }

        private boolean allowed(CommandSender sender) {
            if (!active) {
                return false;
            }
            if (info.permission() == null) {
                return true;
            }
            if (sender instanceof PermissionSubject subject) {
                return permissions.can(subject, info.permission());
            }
            return sender.can(info.permission());
        }

        private boolean matchesArguments(List<String> args) {
            if (!strictArguments) {
                return true;
            }
            var required = 0;
            var max = 0;
            for (var argument : info.arguments()) {
                if (!argument.optional()) {
                    required++;
                }
                max++;
                if (argument.type() == CommandArgumentType.GREEDY_STRING) {
                    max = Integer.MAX_VALUE;
                    break;
                }
            }
            return args.size() >= required && args.size() <= max;
        }

        private boolean active() {
            return active;
        }
    }
}
