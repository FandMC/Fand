package io.fand.api.command;

import io.fand.api.entity.Player;
import io.fand.api.item.ItemType;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Structured data for a command invocation.
 */
public final class CommandContext {

    private final CommandSender sender;
    private final String label;
    private final List<String> args;
    private final Map<String, Object> arguments;

    public CommandContext(CommandSender sender, String label, List<String> args, Map<String, Object> arguments) {
        this.sender = Objects.requireNonNull(sender, "sender");
        this.label = Objects.requireNonNull(label, "label");
        this.args = List.copyOf(Objects.requireNonNull(args, "args"));
        this.arguments = Map.copyOf(Objects.requireNonNull(arguments, "arguments"));
    }

    public CommandSender sender() {
        return sender;
    }

    public <T extends CommandSender> T sender(Class<T> type) {
        return type.cast(sender);
    }

    public String label() {
        return label;
    }

    public List<String> args() {
        return args;
    }

    public Map<String, Object> arguments() {
        return arguments;
    }

    public boolean has(String name) {
        return arguments.containsKey(name);
    }

    public Object argument(String name) {
        var value = arguments.get(name);
        if (value == null) {
            throw new IllegalArgumentException("Missing command argument: " + name);
        }
        return value;
    }

    public <T> T argument(String name, Class<T> type) {
        return type.cast(argument(name));
    }

    public <T> Optional<T> optionalArgument(String name, Class<T> type) {
        var value = arguments.get(name);
        return value == null ? Optional.empty() : Optional.of(type.cast(value));
    }

    public Optional<String> optionalString(String name) {
        return optionalArgument(name, String.class);
    }

    public Optional<Integer> optionalInt(String name) {
        return optionalArgument(name, Integer.class);
    }

    public Optional<Long> optionalLong(String name) {
        return optionalArgument(name, Long.class);
    }

    public Optional<Boolean> optionalBoolean(String name) {
        return optionalArgument(name, Boolean.class);
    }

    public Optional<Double> optionalDouble(String name) {
        return optionalArgument(name, Double.class);
    }

    public Optional<Float> optionalFloat(String name) {
        return optionalArgument(name, Float.class);
    }

    public Optional<Player> optionalPlayer(String name) {
        return optionalArgument(name, Player.class);
    }

    public Optional<ItemType> optionalItem(String name) {
        return optionalArgument(name, ItemType.class);
    }

    public String string(String name) {
        return argument(name, String.class);
    }

    public int intValue(String name) {
        return argument(name, Integer.class);
    }

    public long longValue(String name) {
        return argument(name, Long.class);
    }

    public boolean booleanValue(String name) {
        return argument(name, Boolean.class);
    }

    public double doubleValue(String name) {
        return argument(name, Double.class);
    }

    public float floatValue(String name) {
        return argument(name, Float.class);
    }

    public Player player(String name) {
        return argument(name, Player.class);
    }

    public ItemType item(String name) {
        return argument(name, ItemType.class);
    }
}
