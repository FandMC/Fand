package io.fand.api.item.component;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.List;
import java.util.Objects;

/**
 * Typed value for the vanilla {@code minecraft:custom_model_data} item
 * component.
 */
public record CustomModelData(
        List<Float> floats,
        List<Boolean> flags,
        List<String> strings,
        List<Integer> colors) {

    public static final CustomModelData EMPTY = new CustomModelData(List.of(), List.of(), List.of(), List.of());

    public CustomModelData {
        floats = List.copyOf(Objects.requireNonNull(floats, "floats"));
        flags = List.copyOf(Objects.requireNonNull(flags, "flags"));
        strings = List.copyOf(Objects.requireNonNull(strings, "strings"));
        colors = List.copyOf(Objects.requireNonNull(colors, "colors"));
    }

    public static CustomModelData ofFloat(float value) {
        return new CustomModelData(List.of(value), List.of(), List.of(), List.of());
    }

    public static CustomModelData ofInt(int value) {
        return ofFloat(value);
    }

    public JsonObject toJson() {
        var json = new JsonObject();
        if (!floats.isEmpty()) {
            json.add("floats", arrayOfNumbers(floats));
        }
        if (!flags.isEmpty()) {
            var values = new JsonArray();
            flags.forEach(values::add);
            json.add("flags", values);
        }
        if (!strings.isEmpty()) {
            var values = new JsonArray();
            strings.forEach(values::add);
            json.add("strings", values);
        }
        if (!colors.isEmpty()) {
            json.add("colors", arrayOfNumbers(colors));
        }
        return json;
    }

    public static CustomModelData fromJson(JsonElement value) {
        if (value == null || !value.isJsonObject()) {
            return EMPTY;
        }
        var object = value.getAsJsonObject();
        return new CustomModelData(
                floats(object.get("floats")),
                booleans(object.get("flags")),
                strings(object.get("strings")),
                ints(object.get("colors")));
    }

    private static JsonArray arrayOfNumbers(List<? extends Number> values) {
        var array = new JsonArray();
        values.forEach(array::add);
        return array;
    }

    private static List<Float> floats(JsonElement value) {
        if (value == null || !value.isJsonArray()) {
            return List.of();
        }
        return value.getAsJsonArray().asList().stream()
                .filter(JsonElement::isJsonPrimitive)
                .map(JsonElement::getAsFloat)
                .toList();
    }

    private static List<Integer> ints(JsonElement value) {
        if (value == null || !value.isJsonArray()) {
            return List.of();
        }
        return value.getAsJsonArray().asList().stream()
                .filter(JsonElement::isJsonPrimitive)
                .map(JsonElement::getAsInt)
                .toList();
    }

    private static List<Boolean> booleans(JsonElement value) {
        if (value == null || !value.isJsonArray()) {
            return List.of();
        }
        return value.getAsJsonArray().asList().stream()
                .filter(JsonElement::isJsonPrimitive)
                .map(JsonElement::getAsBoolean)
                .toList();
    }

    private static List<String> strings(JsonElement value) {
        if (value == null || !value.isJsonArray()) {
            return List.of();
        }
        return value.getAsJsonArray().asList().stream()
                .filter(JsonElement::isJsonPrimitive)
                .map(JsonElement::getAsString)
                .toList();
    }
}
