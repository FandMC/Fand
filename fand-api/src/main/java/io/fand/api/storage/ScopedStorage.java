package io.fand.api.storage;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.Map;
import java.util.Optional;

/**
 * Small JSON/KV store for plugin gameplay data.
 *
 * <p><b>Threading:</b> each scope is individually thread-safe; concurrent
 * reads and writes to the same scope (including from async scheduler tasks)
 * are synchronised internally. Different scopes are independent and never
 * contend. {@code set}/{@code remove} mutate in-memory state only;
 * {@link #flush()} is what persists a dirty scope to disk and may block on
 * file I/O. Avoid holding a reference returned from {@link #entries()} across
 * threads; treat it as a point-in-time snapshot.
 */
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

    /** Flushes this scope to its backing store when it has dirty changes. */
    default void flush() {
    }
}
