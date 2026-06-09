package io.fand.api.component;

import com.google.gson.JsonElement;
import java.util.Optional;
import net.kyori.adventure.key.Key;

/**
 * Mutable persistent component access for a live server object.
 *
 * <p>Implementations are backed by live world/server storage. Server
 * implementations resolve reads and writes on the server thread.
 */
public interface DataComponentContainer {

    DataComponentMap snapshot();

    default boolean empty() {
        return snapshot().isEmpty();
    }

    default boolean has(Key key) {
        return snapshot().has(key);
    }

    default boolean has(DataComponentKey<?> key) {
        return has(key.key());
    }

    default Optional<JsonElement> get(Key key) {
        return snapshot().get(key);
    }

    default <T> Optional<T> get(DataComponentKey<T> key) {
        return snapshot().get(key);
    }

    void set(Key key, JsonElement value);

    default <T> void set(DataComponentKey<T> key, T value) {
        set(key.key(), key.encode(value));
    }

    void remove(Key key);

    default void remove(DataComponentKey<?> key) {
        remove(key.key());
    }

    void clear();
}
