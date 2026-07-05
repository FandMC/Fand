package io.fand.api.command;

import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * Read-only metadata for a registered command entry.
 */
public record CommandInfo(
        String namespace,
        String label,
        List<String> path,
        List<CommandArgument> arguments,
        List<String> aliases,
        @Nullable String permission
) {
    public CommandInfo {
        namespace = Objects.requireNonNull(namespace, "namespace");
        label = Objects.requireNonNull(label, "label");
        path = List.copyOf(Objects.requireNonNull(path, "path"));
        arguments = List.copyOf(Objects.requireNonNull(arguments, "arguments"));
        aliases = List.copyOf(Objects.requireNonNull(aliases, "aliases"));
    }

    public String usage() {
        var usage = new StringBuilder();
        if (!namespace.isEmpty()) {
            usage.append(namespace).append(':');
        }
        usage.append(label);
        for (var segment : path) {
            usage.append(' ').append(segment);
        }
        for (var argument : arguments) {
            usage.append(' ')
                    .append(argument.optional() ? '[' : '<')
                    .append(argument.name())
                    .append(argument.optional() ? ']' : '>');
        }
        return usage.toString();
    }
}
