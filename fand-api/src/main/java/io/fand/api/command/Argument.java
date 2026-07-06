package io.fand.api.command;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import net.kyori.adventure.key.Key;
import org.jspecify.annotations.Nullable;

/**
 * Typed argument definition used by the command builder and annotation runtime.
 *
 * @param <T> parsed value type
 */
public final class Argument<T> {

    private final Class<T> valueType;
    private final CommandArgumentType type;
    private final ArgumentParser<? extends T> parser;
    private final boolean optional;
    private final @Nullable T defaultValue;
    private final boolean optionalSender;
    private final List<String> suggestions;
    private final @Nullable Key registry;

    Argument(
            Class<T> valueType,
            CommandArgumentType type,
            ArgumentParser<? extends T> parser,
            boolean optional,
            @Nullable T defaultValue,
            boolean optionalSender,
            Collection<String> suggestions,
            @Nullable Key registry
    ) {
        this.valueType = Objects.requireNonNull(valueType, "valueType");
        this.type = Objects.requireNonNull(type, "type");
        this.parser = Objects.requireNonNull(parser, "parser");
        this.optional = optional;
        this.defaultValue = defaultValue;
        this.optionalSender = optionalSender;
        this.suggestions = List.copyOf(Objects.requireNonNull(suggestions, "suggestions"));
        this.registry = registry;
    }

    public Class<T> valueType() {
        return valueType;
    }

    public CommandArgumentType type() {
        return type;
    }

    public T parse(String input) throws Exception {
        return parser.parse(input);
    }

    public boolean optional() {
        return optional;
    }

    public @Nullable T defaultValue() {
        return defaultValue;
    }

    public boolean usesSenderAsDefault() {
        return optionalSender;
    }

    public List<String> suggestions() {
        return suggestions;
    }

    public @Nullable Key registry() {
        return registry;
    }

    public Argument<T> asOptional() {
        return new Argument<>(valueType, type, parser, true, defaultValue, optionalSender, suggestions, registry);
    }

    public Argument<T> asOptional(T defaultValue) {
        return new Argument<>(valueType, type, parser, true, Objects.requireNonNull(defaultValue, "defaultValue"), optionalSender, suggestions, registry);
    }

    public Argument<T> optionalSender() {
        return new Argument<>(valueType, type, parser, true, defaultValue, true, suggestions, registry);
    }

    public Argument<T> suggests(String first, String... rest) {
        var values = new java.util.ArrayList<String>(1 + rest.length);
        values.add(Objects.requireNonNull(first, "first"));
        for (var value : rest) {
            values.add(Objects.requireNonNull(value, "suggestion"));
        }
        return suggests(values);
    }

    public Argument<T> suggests(Collection<String> suggestions) {
        return new Argument<>(valueType, type, parser, optional, defaultValue, optionalSender, suggestions, registry);
    }

    public CommandArgument metadata(String name) {
        return new CommandArgument(name, type, optional, suggestions, registry);
    }
}
