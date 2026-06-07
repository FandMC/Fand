package io.fand.api.item.component;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.kyori.adventure.key.Key;

final class ItemComponentJson {

    private ItemComponentJson() {
    }

    static JsonObject object(JsonElement value, String name) {
        if (value == null || !value.isJsonObject()) {
            throw new IllegalArgumentException(name + " must be a JSON object");
        }
        return value.getAsJsonObject();
    }

    static JsonObject objectOrEmpty(JsonElement value) {
        return value != null && value.isJsonObject() ? value.getAsJsonObject() : new JsonObject();
    }

    static JsonObject copyObject(JsonObject value) {
        return value == null ? new JsonObject() : value.deepCopy();
    }

    static Optional<JsonObject> optionalObject(JsonObject object, String property) {
        var value = object.get(property);
        return value != null && value.isJsonObject()
                ? Optional.of(value.getAsJsonObject().deepCopy())
                : Optional.empty();
    }

    static Optional<String> optionalString(JsonObject object, String property) {
        var value = object.get(property);
        return value != null && value.isJsonPrimitive()
                ? Optional.of(value.getAsString())
                : Optional.empty();
    }

    static Optional<Key> optionalKey(JsonObject object, String property) {
        return optionalString(object, property).map(Key::key);
    }

    static Optional<Integer> optionalInt(JsonObject object, String property) {
        var value = object.get(property);
        return value != null && value.isJsonPrimitive()
                ? Optional.of(value.getAsInt())
                : Optional.empty();
    }

    static Optional<Long> optionalLong(JsonObject object, String property) {
        var value = object.get(property);
        return value != null && value.isJsonPrimitive()
                ? Optional.of(value.getAsLong())
                : Optional.empty();
    }

    static Optional<Float> optionalFloat(JsonObject object, String property) {
        var value = object.get(property);
        return value != null && value.isJsonPrimitive()
                ? Optional.of(value.getAsFloat())
                : Optional.empty();
    }

    static Optional<Double> optionalDouble(JsonObject object, String property) {
        var value = object.get(property);
        return value != null && value.isJsonPrimitive()
                ? Optional.of(value.getAsDouble())
                : Optional.empty();
    }

    static boolean booleanOr(JsonObject object, String property, boolean fallback) {
        var value = object.get(property);
        return value != null && value.isJsonPrimitive() ? value.getAsBoolean() : fallback;
    }

    static int intOr(JsonObject object, String property, int fallback) {
        return optionalInt(object, property).orElse(fallback);
    }

    static long longOr(JsonObject object, String property, long fallback) {
        return optionalLong(object, property).orElse(fallback);
    }

    static float floatOr(JsonObject object, String property, float fallback) {
        return optionalFloat(object, property).orElse(fallback);
    }

    static double doubleOr(JsonObject object, String property, double fallback) {
        return optionalDouble(object, property).orElse(fallback);
    }

    static Key key(JsonObject object, String property) {
        var value = object.get(property);
        if (value == null || !value.isJsonPrimitive()) {
            throw new IllegalArgumentException(property + " must be a key string");
        }
        return Key.key(value.getAsString());
    }

    static List<Key> keys(JsonElement value) {
        if (value == null) {
            return List.of();
        }
        if (value.isJsonPrimitive()) {
            return List.of(Key.key(value.getAsString()));
        }
        if (!value.isJsonArray()) {
            return List.of();
        }
        var keys = new ArrayList<Key>();
        for (var element : value.getAsJsonArray()) {
            if (element.isJsonPrimitive()) {
                keys.add(Key.key(element.getAsString()));
            }
        }
        return List.copyOf(keys);
    }

    static JsonArray keyArray(List<Key> values) {
        var array = new JsonArray();
        values.forEach(value -> array.add(value.asString()));
        return array;
    }

    static JsonElement stringOrArray(List<String> values) {
        if (values.size() == 1) {
            return new JsonPrimitive(values.getFirst());
        }
        var array = new JsonArray();
        values.forEach(array::add);
        return array;
    }

    static List<String> strings(JsonElement value) {
        if (value == null) {
            return List.of();
        }
        if (value.isJsonPrimitive()) {
            return List.of(value.getAsString());
        }
        if (!value.isJsonArray()) {
            return List.of();
        }
        var strings = new ArrayList<String>();
        for (var element : value.getAsJsonArray()) {
            if (element.isJsonPrimitive()) {
                strings.add(element.getAsString());
            }
        }
        return List.copyOf(strings);
    }

    static JsonObject withId(JsonObject data, Key id) {
        var copy = copyObject(data);
        copy.addProperty("id", id.asString());
        return copy;
    }

    static JsonObject withoutId(JsonObject data) {
        var copy = copyObject(data);
        copy.remove("id");
        return copy;
    }
}
