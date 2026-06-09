package io.fand.api.storage;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.Map;
import java.util.Optional;

/** Small JSON/KV store for plugin gameplay data. */
public interface ScopedStorage {

    Optional<JsonElement> get(String key);

    default Optional<String> getString(String key) {
        return get(key)
                .filter(JsonElement::isJsonPrimitive)
                .map(JsonElement::getAsString);
    }

    default Optional<Integer> getInt(String key) {
        return get(key)
                .filter(JsonElement::isJsonPrimitive)
                .map(JsonElement::getAsInt);
    }

    default Optional<Boolean> getBoolean(String key) {
        return get(key)
                .filter(JsonElement::isJsonPrimitive)
                .map(JsonElement::getAsBoolean);
    }

    Map<String, JsonElement> entries();

    void set(String key, JsonElement value);

    default void setString(String key, String value) {
        set(key, new com.google.gson.JsonPrimitive(value));
    }

    default void setInt(String key, int value) {
        set(key, new com.google.gson.JsonPrimitive(value));
    }

    default void setBoolean(String key, boolean value) {
        set(key, new com.google.gson.JsonPrimitive(value));
    }

    void remove(String key);

    void clear();

    JsonObject toJson();
}
