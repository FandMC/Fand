package io.fand.api.region;

import com.google.gson.JsonElement;
import java.util.Objects;

/**
 * Codec for serializing region flag values.
 */
public interface RegionFlagCodec<T> {

    JsonElement encode(T value);

    T decode(JsonElement json);

    static RegionFlagCodec<Boolean> bool() {
        return new RegionFlagCodec<>() {
            @Override
            public JsonElement encode(Boolean value) {
                return new com.google.gson.JsonPrimitive(Objects.requireNonNull(value, "value"));
            }

            @Override
            public Boolean decode(JsonElement json) {
                requirePrimitive("boolean flag", json);
                return json.getAsBoolean();
            }
        };
    }

    static RegionFlagCodec<Integer> integer() {
        return new RegionFlagCodec<>() {
            @Override
            public JsonElement encode(Integer value) {
                return new com.google.gson.JsonPrimitive(Objects.requireNonNull(value, "value"));
            }

            @Override
            public Integer decode(JsonElement json) {
                requirePrimitive("integer flag", json);
                return json.getAsInt();
            }
        };
    }

    static RegionFlagCodec<String> string() {
        return new RegionFlagCodec<>() {
            @Override
            public JsonElement encode(String value) {
                return new com.google.gson.JsonPrimitive(Objects.requireNonNull(value, "value"));
            }

            @Override
            public String decode(JsonElement json) {
                requirePrimitive("string flag", json);
                return json.getAsString();
            }
        };
    }

    private static void requirePrimitive(String label, JsonElement json) {
        if (json == null || !json.isJsonPrimitive()) {
            throw new IllegalArgumentException(label + " must be a JSON primitive");
        }
    }
}
