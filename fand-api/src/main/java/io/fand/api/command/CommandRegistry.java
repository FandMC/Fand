package io.fand.api.command;

import java.util.List;
import java.util.Optional;

/**
 * Registry for server commands. Plugin commands should be registered during
 * {@link io.fand.api.plugin.Plugin#onEnable} and unregistered automatically on
 * disable.
 */
public interface CommandRegistry {

    default CommandRegistration register(Object command) {
        throw new UnsupportedOperationException("This command registry does not support annotated command registration");
    }

    CommandRegistration register(CommandDescriptor descriptor, CommandExecutor executor, CommandCompleter completer);

    default CommandRegistration register(CommandDescriptor descriptor, CommandExecutor executor) {
        return register(descriptor, executor, (sender, label, args) -> List.of());
    }

    Optional<RegisteredCommand> lookup(String name);

    boolean claims(List<String> tokens);

    Optional<ResolvedCommand> resolve(CommandSender sender, List<String> tokens);

    List<String> suggestions(CommandSender sender, List<String> tokens);

    List<RegisteredCommand> visibleCommands(CommandSender sender);
}
