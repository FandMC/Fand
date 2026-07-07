package io.fand.api.command;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Registry for server commands. Plugin commands should be registered during
 * {@link io.fand.api.plugin.Plugin#onEnable} and unregistered automatically on
 * disable.
 */
public interface CommandRegistry {

    default CommandRegistration register(Object command) {
        throw new UnsupportedOperationException("This command registry does not support annotated command registration");
    }

    default CommandRegistration register(String label, Consumer<CommandBuilder> builder) {
        return register(CommandBuilder.define(label, builder));
    }

    default CommandRegistration register(CommandBuilder builder) {
        return register(builder.build());
    }

    CommandRegistration register(CommandDefinition definition);

    Optional<CommandInfo> lookup(String name);

    boolean claims(List<String> tokens);

    List<String> suggestions(CommandSender sender, List<String> tokens);

    List<CommandInfo> visibleCommands(CommandSender sender);
}
