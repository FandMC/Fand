package io.fand.api.command;

import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * Static metadata for a registered command.
 *
 * @param namespace command namespace, typically the owning plugin id
 * @param label canonical root command name without namespace
 * @param subcommands literal subcommand path after the root command
 * @param aliases alternate root command names in the same namespace
 * @param permission optional permission required to execute or complete the command
 */
public record CommandDescriptor(
        String namespace,
        String label,
        List<String> subcommands,
        List<String> aliases,
        @Nullable String permission
) {
    public CommandDescriptor {
        subcommands = List.copyOf(subcommands);
        aliases = List.copyOf(aliases);
    }

    public CommandDescriptor(String namespace, String label, List<String> aliases) {
        this(namespace, label, List.of(), aliases, null);
    }
}
