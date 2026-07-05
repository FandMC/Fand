package io.fand.server.plugin;

import io.fand.api.command.CommandDefinition;
import io.fand.api.command.CommandInfo;
import io.fand.api.command.CommandNode;
import io.fand.api.command.CommandRegistration;
import io.fand.api.command.CommandRegistry;
import io.fand.api.command.CommandSender;
import io.fand.server.command.AnnotatedCommands;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class PluginCommandRegistry implements CommandRegistry {

    private final CommandRegistry delegate;
    private final PluginResourceTracker tracker;
    private final String namespace;

    public PluginCommandRegistry(CommandRegistry delegate, PluginResourceTracker tracker, String namespace) {
        this.delegate = delegate;
        this.tracker = tracker;
        this.namespace = namespace;
    }

    public String namespace() {
        return namespace;
    }

    @Override
    public CommandRegistration register(Object command) {
        return AnnotatedCommands.register(this, command);
    }

    @Override
    public CommandRegistration register(CommandDefinition definition) {
        var scoped = new CommandDefinition(
                namespace,
                definition.label(),
                definition.aliases(),
                definition.permission(),
                definition.root()
        );
        return tracker.track(delegate.register(scoped), descriptors(scoped));
    }

    @Override
    public Optional<CommandInfo> lookup(String name) {
        return delegate.lookup(name).filter(this::ownedByThisPlugin);
    }

    @Override
    public boolean claims(List<String> tokens) {
        return delegate.claims(tokens);
    }

    @Override
    public List<String> suggestions(CommandSender sender, List<String> tokens) {
        return delegate.suggestions(sender, tokens);
    }

    @Override
    public List<CommandInfo> visibleCommands(CommandSender sender) {
        var filtered = new ArrayList<CommandInfo>();
        for (var command : delegate.visibleCommands(sender)) {
            if (ownedByThisPlugin(command)) {
                filtered.add(command);
            }
        }
        return List.copyOf(filtered);
    }

    private boolean ownedByThisPlugin(CommandInfo command) {
        return namespace.equals(command.namespace());
    }

    private static List<CommandInfo> descriptors(CommandDefinition definition) {
        var descriptors = new ArrayList<CommandInfo>();
        collect(definition, definition.root(), definition.permission(), List.of(), List.of(), descriptors);
        return descriptors;
    }

    private static void collect(
            CommandDefinition definition,
            CommandNode node,
            String inheritedPermission,
            List<String> path,
            List<io.fand.api.command.CommandArgument> arguments,
            List<CommandInfo> descriptors
    ) {
        var permission = node.permission() == null ? inheritedPermission : node.permission();
        if (node.action() != null) {
            descriptors.add(new CommandInfo(
                    definition.namespace(),
                    definition.label(),
                    path,
                    arguments,
                    definition.aliases(),
                    permission));
        }
        for (var child : node.children()) {
            switch (child.type()) {
                case LITERAL -> {
                    var nextPath = new ArrayList<>(path);
                    nextPath.add(child.name());
                    collect(definition, child, permission, nextPath, arguments, descriptors);
                }
                case ARGUMENT -> {
                    var nextArguments = new ArrayList<>(arguments);
                    nextArguments.add(child.argument().metadata(child.name()));
                    collect(definition, child, permission, path, nextArguments, descriptors);
                }
                case ROOT -> collect(definition, child, permission, path, arguments, descriptors);
            }
        }
    }
}
