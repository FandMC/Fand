package io.fand.api.command;

import io.fand.api.Fand;
import io.fand.api.entity.Player;
import io.fand.api.item.ItemType;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import net.kyori.adventure.key.Key;

/**
 * Factory methods for common command argument types.
 */
public final class Arguments {

    private Arguments() {
    }

    public static Argument<String> word() {
        return string(CommandArgumentType.WORD);
    }

    public static Argument<String> string() {
        return string(CommandArgumentType.STRING);
    }

    public static Argument<String> greedyString() {
        return string(CommandArgumentType.GREEDY_STRING);
    }

    public static Argument<Boolean> bool() {
        return new Argument<>(Boolean.class, CommandArgumentType.BOOLEAN, input -> {
            var value = input.toLowerCase(Locale.ROOT);
            if ("true".equals(value)) {
                return true;
            }
            if ("false".equals(value)) {
                return false;
            }
            throw new IllegalArgumentException("Expected true or false, got " + input);
        }, false, null, false, List.of("true", "false"), null);
    }

    public static Argument<Integer> integer() {
        return integer(Integer.MIN_VALUE, Integer.MAX_VALUE);
    }

    public static Argument<Integer> integer(int min, int max) {
        return new Argument<>(Integer.class, CommandArgumentType.INTEGER, input -> {
            var value = Integer.parseInt(input);
            if (value < min || value > max) {
                throw new IllegalArgumentException("Integer must be between " + min + " and " + max + ": " + input);
            }
            return value;
        }, false, null, false, List.of(), null);
    }

    public static Argument<Long> longValue() {
        return longValue(Long.MIN_VALUE, Long.MAX_VALUE);
    }

    public static Argument<Long> longValue(long min, long max) {
        return new Argument<>(Long.class, CommandArgumentType.LONG, input -> {
            var value = Long.parseLong(input);
            if (value < min || value > max) {
                throw new IllegalArgumentException("Long must be between " + min + " and " + max + ": " + input);
            }
            return value;
        }, false, null, false, List.of(), null);
    }

    public static Argument<Float> floatValue() {
        return new Argument<>(Float.class, CommandArgumentType.FLOAT, Float::parseFloat, false, null, false, List.of(), null);
    }

    public static Argument<Double> doubleValue() {
        return new Argument<>(Double.class, CommandArgumentType.DOUBLE, Double::parseDouble, false, null, false, List.of(), null);
    }

    public static Argument<Player> player() {
        return new Argument<>(Player.class, CommandArgumentType.PLAYER, input ->
                Fand.server().player(input).orElseThrow(() -> new IllegalArgumentException("Unknown player: " + input)),
                false, null, false, List.of(), null);
    }

    public static Argument<ItemType> item() {
        return new Argument<>(ItemType.class, CommandArgumentType.REGISTRY_KEY, input ->
                Fand.server().itemType(Key.key(input)).orElseThrow(() -> new IllegalArgumentException("Unknown item: " + input)),
                false, null, false, List.of(), Key.key("minecraft:item"));
    }

    public static Argument<String> enumValue(String first, String... rest) {
        var values = new java.util.ArrayList<String>(1 + rest.length);
        values.add(first);
        values.addAll(List.of(rest));
        return enumValue(values);
    }

    public static Argument<String> enumValue(Collection<String> values) {
        var normalized = values.stream()
                .map(value -> value.toLowerCase(Locale.ROOT))
                .toList();
        return new Argument<>(String.class, CommandArgumentType.ENUM, input -> {
            var value = input.toLowerCase(Locale.ROOT);
            if (!normalized.contains(value)) {
                throw new IllegalArgumentException("Expected one of " + normalized + ", got " + input);
            }
            return value;
        }, false, null, false, normalized, null);
    }

    private static Argument<String> string(CommandArgumentType type) {
        return new Argument<>(String.class, type, input -> input, false, null, false, List.of(), null);
    }
}
