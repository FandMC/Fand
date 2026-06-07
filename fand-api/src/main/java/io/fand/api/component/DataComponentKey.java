package io.fand.api.component;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import net.kyori.adventure.key.Key;

/**
 * A typed key for persistent block/entity components.
 *
 * <p>The key owns both serialization directions, so plugins can expose domain
 * values directly instead of passing around untyped payload wrappers.
 */
public record DataComponentKey<T>(Key key, DataComponentCodec<T> codec) {

    public DataComponentKey {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(codec, "codec");
    }

    public static <T> DataComponentKey<T> of(Key key, DataComponentCodec<T> codec) {
        return new DataComponentKey<>(key, codec);
    }

    public static <T> DataComponentKey<T> of(
            Key key,
            Function<? super T, ? extends JsonElement> encoder,
            Function<? super JsonElement, ? extends T> decoder) {
        Objects.requireNonNull(encoder, "encoder");
        Objects.requireNonNull(decoder, "decoder");
        return of(key, new DataComponentCodec<>() {
            @Override
            public JsonElement encode(T value) {
                return Objects.requireNonNull(encoder.apply(value), "encoded component value").deepCopy();
            }

            @Override
            public T decode(JsonElement json) {
                return Objects.requireNonNull(decoder.apply(json.deepCopy()), "decoded component value");
            }
        });
    }

    public static DataComponentKey<JsonElement> json(Key key) {
        return of(key, new DataComponentCodec<>() {
            @Override
            public JsonElement encode(JsonElement value) {
                return Objects.requireNonNull(value, "value").deepCopy();
            }

            @Override
            public JsonElement decode(JsonElement json) {
                return Objects.requireNonNull(json, "json").deepCopy();
            }
        });
    }

    public static DataComponentKey<JsonObject> object(Key key) {
        return of(key, value -> Objects.requireNonNull(value, "value").deepCopy(), json -> {
            if (json == null || !json.isJsonObject()) {
                throw new IllegalArgumentException(key.asString() + " must be a JSON object");
            }
            return json.getAsJsonObject().deepCopy();
        });
    }

    public static DataComponentKey<String> string(Key key) {
        return of(key, value -> new JsonPrimitive(Objects.requireNonNull(value, "value")), json -> {
            requirePrimitive(key, json);
            return json.getAsString();
        });
    }

    public static DataComponentKey<Boolean> bool(Key key) {
        return of(key, JsonPrimitive::new, json -> {
            requirePrimitive(key, json);
            return json.getAsBoolean();
        });
    }

    public static DataComponentKey<Integer> integer(Key key) {
        return of(key, JsonPrimitive::new, json -> {
            requirePrimitive(key, json);
            return json.getAsInt();
        });
    }

    public static DataComponentKey<Long> longValue(Key key) {
        return of(key, JsonPrimitive::new, json -> {
            requirePrimitive(key, json);
            return json.getAsLong();
        });
    }

    public static DataComponentKey<Double> doubleValue(Key key) {
        return of(key, JsonPrimitive::new, json -> {
            requirePrimitive(key, json);
            return json.getAsDouble();
        });
    }

    public static DataComponentKey<Key> key(Key key) {
        return of(key, value -> new JsonPrimitive(Objects.requireNonNull(value, "value").asString()), json -> {
            requirePrimitive(key, json);
            return Key.key(json.getAsString());
        });
    }

    public static DataComponentKey<UUID> uuid(Key key) {
        return of(key, value -> new JsonPrimitive(Objects.requireNonNull(value, "value").toString()), json -> {
            requirePrimitive(key, json);
            return UUID.fromString(json.getAsString());
        });
    }

    JsonElement encode(T value) {
        return Objects.requireNonNull(codec.encode(Objects.requireNonNull(value, "value")), "encoded component value")
                .deepCopy();
    }

    T decode(JsonElement json) {
        return codec.decode(Objects.requireNonNull(json, "json").deepCopy());
    }

    private static void requirePrimitive(Key key, JsonElement json) {
        if (json == null || !json.isJsonPrimitive()) {
            throw new IllegalArgumentException(key.asString() + " must be a JSON primitive");
        }
    }
}
