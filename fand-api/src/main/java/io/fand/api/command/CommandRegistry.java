package io.fand.api.command;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Registry for server commands. Plugin commands should be registered during
 * {@link io.fand.api.plugin.Plugin#onEnable} and unregistered automatically on
 * disable.
 *
 * <p>Commands remain available through {@code namespace:label}, including their
 * aliases. When namespaces share a local label or alias, its first registered
 * owner handles that short name until unregistered, then the next owner takes
 * over. Permission denial does not fall through to another owner. Duplicate
 * paths within the same namespace are rejected.
 *
 * <p>Registration and unregistration refresh online players' command trees on
 * the next server tick; changes within a tick are coalesced.
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
