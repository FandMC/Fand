package io.fand.server.command;

import io.fand.api.command.CommandCompleter;
import io.fand.api.command.CommandDescriptor;
import io.fand.api.command.CommandExecutor;
import io.fand.api.command.CommandRegistration;
import io.fand.api.command.CommandRegistry;
import io.fand.api.command.CommandSpec;
import java.util.List;
import java.util.Objects;

public final class AnnotatedCommands {

    private AnnotatedCommands() {
    }

    public static CommandRegistration register(CommandRegistry registry, Object command) {
        Objects.requireNonNull(registry, "registry");
        Objects.requireNonNull(command, "command");

        var spec = command.getClass().getAnnotation(CommandSpec.class);
        if (spec == null) {
            throw new IllegalArgumentException("Command class is missing @CommandSpec: " + command.getClass().getName());
        }
        var namespace = spec.namespace().isBlank()
                ? registry instanceof io.fand.server.plugin.PluginCommandRegistry pluginRegistry ? pluginRegistry.namespace() : ""
                : spec.namespace();
        if (namespace.isBlank()) {
            throw new IllegalArgumentException("A namespace is required for command class " + command.getClass().getName());
        }
        return register(registry, namespace, command);
    }

    public static CommandRegistration register(CommandRegistry registry, String namespace, Object command) {
        Objects.requireNonNull(registry, "registry");
        Objects.requireNonNull(namespace, "namespace");
        Objects.requireNonNull(command, "command");

        var spec = command.getClass().getAnnotation(CommandSpec.class);
        if (spec == null) {
            throw new IllegalArgumentException("Command class is missing @CommandSpec: " + command.getClass().getName());
        }
        if (!(command instanceof CommandExecutor executor)) {
            throw new IllegalArgumentException("Command class must implement CommandExecutor: " + command.getClass().getName());
        }
        CommandCompleter completer = command instanceof CommandCompleter value ? value : (sender, label, args) -> List.of();
        var permission = spec.permission().isBlank() ? null : spec.permission();
        var descriptor = new CommandDescriptor(namespace, spec.label(), List.of(spec.subcommands()), List.of(spec.aliases()), permission);
        return registry.register(descriptor, executor, completer);
    }
}
