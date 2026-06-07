package io.fand.api.component;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import net.kyori.adventure.key.Key;

/**
 * Immutable snapshot of persistent Fand data components.
 *
 * <p>Unlike item data components this is not a patch model: absence means the
 * component is not present, and removing a component deletes it from storage.
 */
public record DataComponentMap(Map<Key, JsonElement> values) {

    public static final DataComponentMap EMPTY = new DataComponentMap(Map.of());

    public DataComponentMap {
        Objects.requireNonNull(values, "values");
        var copied = new LinkedHashMap<Key, JsonElement>();
        for (var entry : values.entrySet()) {
            copied.put(
                    Objects.requireNonNull(entry.getKey(), "component key"),
                    Objects.requireNonNull(entry.getValue(), "component value").deepCopy());
        }
        values = Collections.unmodifiableMap(copied);
    }

    public static DataComponentMap empty() {
        return EMPTY;
    }

    public static DataComponentMap of(Key key, JsonElement value) {
        return EMPTY.with(key, value);
    }

    public static <T> DataComponentMap of(DataComponentKey<T> key, T value) {
        return EMPTY.with(key, value);
    }

    public static DataComponentMap fromJson(String json) {
        return fromJson(JsonParser.parseString(json));
    }

    public static DataComponentMap fromJson(JsonElement json) {
        if (json == null || !json.isJsonObject()) {
            throw new IllegalArgumentException("Data component map must be a JSON object");
        }
        var values = new LinkedHashMap<Key, JsonElement>();
        for (var entry : json.getAsJsonObject().entrySet()) {
            values.put(Key.key(entry.getKey()), entry.getValue().deepCopy());
        }
        return new DataComponentMap(values);
    }

    @Override
    public Map<Key, JsonElement> values() {
        var copied = new LinkedHashMap<Key, JsonElement>();
        values.forEach((key, value) -> copied.put(key, value.deepCopy()));
        return Collections.unmodifiableMap(copied);
    }

    public boolean isEmpty() {
        return values.isEmpty();
    }

    public Set<Key> keys() {
        return values.keySet();
    }

    public boolean has(Key key) {
        return values.containsKey(key);
    }

    public boolean has(DataComponentKey<?> key) {
        return has(key.key());
    }

    public Optional<JsonElement> get(Key key) {
        var value = values.get(key);
        return value == null ? Optional.empty() : Optional.of(value.deepCopy());
    }

    public <T> Optional<T> get(DataComponentKey<T> key) {
        Objects.requireNonNull(key, "key");
        return get(key.key()).map(key::decode);
    }

    public DataComponentMap with(Key key, JsonElement value) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(value, "value");
        var next = new LinkedHashMap<>(values);
        next.put(key, value.deepCopy());
        return new DataComponentMap(next);
    }

    public <T> DataComponentMap with(DataComponentKey<T> key, T value) {
        Objects.requireNonNull(key, "key");
        return with(key.key(), key.encode(value));
    }

    public DataComponentMap without(Key key) {
        Objects.requireNonNull(key, "key");
        if (!values.containsKey(key)) {
            return this;
        }
        var next = new LinkedHashMap<>(values);
        next.remove(key);
        return new DataComponentMap(next);
    }

    public DataComponentMap without(DataComponentKey<?> key) {
        Objects.requireNonNull(key, "key");
        return without(key.key());
    }

    public DataComponentMap apply(DataComponentMap patch) {
        Objects.requireNonNull(patch, "patch");
        if (patch.isEmpty()) {
            return this;
        }
        var next = new LinkedHashMap<>(values);
        patch.values.forEach((key, value) -> next.put(key, value.deepCopy()));
        return new DataComponentMap(next);
    }

    public JsonObject toJson() {
        var json = new JsonObject();
        for (var entry : values.entrySet()) {
            json.add(entry.getKey().asString(), entry.getValue().deepCopy());
        }
        return json;
    }
}
