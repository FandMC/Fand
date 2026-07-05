package io.fand.api.command;

import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * Immutable command tree node produced by {@link CommandBuilder}.
 */
public record CommandNode(
        CommandNodeType type,
        String name,
        @Nullable Argument<?> argument,
        @Nullable String permission,
        @Nullable CommandAction action,
        @Nullable CommandSuggestionProvider suggestions,
        List<CommandNode> children
) {
    public CommandNode {
        type = Objects.requireNonNull(type, "type");
        name = Objects.requireNonNull(name, "name");
        children = List.copyOf(Objects.requireNonNull(children, "children"));
        if (type == CommandNodeType.ARGUMENT && argument == null) {
            throw new IllegalArgumentException("argument node requires an argument definition");
        }
        if (type != CommandNodeType.ARGUMENT && argument != null) {
            throw new IllegalArgumentException("only argument nodes can carry an argument definition");
        }
    }
}
