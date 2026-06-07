package io.fand.api.component;

import com.google.gson.JsonElement;

/**
 * Converts a typed component value to and from JSON for persistent Fand block
 * and entity component storage.
 */
public interface DataComponentCodec<T> {

    JsonElement encode(T value);

    T decode(JsonElement json);
}
