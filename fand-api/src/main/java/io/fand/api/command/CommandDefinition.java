package io.fand.api.command;

import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * Immutable command definition registered by the new command API.
 */
public record CommandDefinition(
        String namespace,
        String label,
        List<String> aliases,
        @Nullable String permission,
        CommandNode root
) {
    public CommandDefinition {
        namespace = Objects.requireNonNull(namespace, "namespace");
        label = Objects.requireNonNull(label, "label");
        aliases = List.copyOf(Objects.requireNonNull(aliases, "aliases"));
        root = Objects.requireNonNull(root, "root");
    }
}
