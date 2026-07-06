package io.fand.api.command;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import org.jspecify.annotations.Nullable;

/**
 * Fluent builder for structured commands.
 */
public final class CommandBuilder {

    private final State state;
    private final MutableNode current;

    public CommandBuilder(String label) {
        this.state = new State(Objects.requireNonNull(label, "label"));
        this.current = state.root;
    }

    private CommandBuilder(State state, MutableNode current) {
        this.state = state;
        this.current = current;
    }

    public CommandBuilder namespace(String namespace) {
        state.namespace = Objects.requireNonNull(namespace, "namespace");
        return this;
    }

    public CommandBuilder aliases(String first, String... rest) {
        state.aliases.add(Objects.requireNonNull(first, "first"));
        for (var alias : rest) {
            state.aliases.add(Objects.requireNonNull(alias, "alias"));
        }
        return this;
    }

    public CommandBuilder aliases(Collection<String> aliases) {
        state.aliases.addAll(List.copyOf(Objects.requireNonNull(aliases, "aliases")));
        return this;
    }

    public CommandBuilder permission(String permission) {
        var normalized = Objects.requireNonNull(permission, "permission");
        if (current == state.root) {
            state.permission = normalized;
        } else {
            current.permission = normalized;
        }
        return this;
    }

    public CommandBuilder executes(CommandAction action) {
        current.action = Objects.requireNonNull(action, "action");
        return this;
    }

    public CommandBuilder suggests(CommandSuggestionProvider suggestions) {
        current.suggestions = Objects.requireNonNull(suggestions, "suggestions");
        return this;
    }

    public CommandBuilder literal(String name) {
        return new CommandBuilder(state, current.literal(name));
    }

    public CommandBuilder literal(String name, Consumer<CommandBuilder> branch) {
        Objects.requireNonNull(branch, "branch").accept(literal(name));
        return this;
    }

    public CommandBuilder argument(String name, Argument<?> argument) {
        return new CommandBuilder(state, current.argument(name, argument));
    }

    public CommandBuilder argument(String name, Argument<?> argument, Consumer<CommandBuilder> branch) {
        Objects.requireNonNull(branch, "branch").accept(argument(name, argument));
        return this;
    }

    public CommandDefinition build() {
        return new CommandDefinition(
                state.namespace == null ? "" : state.namespace,
                state.label,
                state.aliases,
                state.permission,
                state.root.toImmutable());
    }

    private static final class State {
        private final String label;
        private final MutableNode root = MutableNode.root();
        private final ArrayList<String> aliases = new ArrayList<>();
        private @Nullable String namespace;
        private @Nullable String permission;

        private State(String label) {
            this.label = label;
        }
    }

    private static final class MutableNode {
        private final CommandNodeType type;
        private final String name;
        private final @Nullable Argument<?> argument;
        private final LinkedHashMap<String, MutableNode> children = new LinkedHashMap<>();
        private @Nullable String permission;
        private @Nullable CommandAction action;
        private @Nullable CommandSuggestionProvider suggestions;

        private MutableNode(CommandNodeType type, String name, @Nullable Argument<?> argument) {
            this.type = type;
            this.name = name;
            this.argument = argument;
        }

        private static MutableNode root() {
            return new MutableNode(CommandNodeType.ROOT, "", null);
        }

        private MutableNode literal(String name) {
            var checked = Objects.requireNonNull(name, "name");
            var key = "literal:" + checked;
            var existing = children.get(key);
            if (existing != null) {
                return existing;
            }
            var node = new MutableNode(CommandNodeType.LITERAL, checked, null);
            children.put(key, node);
            return node;
        }

        private MutableNode argument(String name, Argument<?> argument) {
            Objects.requireNonNull(argument, "argument");
            var checked = Objects.requireNonNull(name, "name");
            var key = "argument:" + checked;
            var existing = children.get(key);
            if (existing != null) {
                if (!sameArgumentDefinition(existing.argument, argument)) {
                    throw new IllegalArgumentException("Argument node already exists with a different definition: " + checked);
                }
                return existing;
            }
            var node = new MutableNode(CommandNodeType.ARGUMENT, checked, argument);
            children.put(key, node);
            return node;
        }

        private static boolean sameArgumentDefinition(@Nullable Argument<?> first, Argument<?> second) {
            if (first == null) {
                return false;
            }
            return first.valueType().equals(second.valueType())
                    && first.type() == second.type()
                    && first.optional() == second.optional()
                    && Objects.equals(first.defaultValue(), second.defaultValue())
                    && first.usesSenderAsDefault() == second.usesSenderAsDefault()
                    && first.suggestions().equals(second.suggestions())
                    && Objects.equals(first.registry(), second.registry());
        }

        private CommandNode toImmutable() {
            return new CommandNode(
                    type,
                    name,
                    argument,
                    permission,
                    action,
                    suggestions,
                    children.values().stream()
                            .map(MutableNode::toImmutable)
                            .toList());
        }
    }
}
