package io.fand.api.region;

import com.google.gson.JsonElement;
import java.util.Objects;
import net.kyori.adventure.key.Key;

/**
 * Typed flag definition available to regions.
 */
public record RegionFlag<T>(Key key, RegionFlagCodec<T> codec, T defaultValue) {

    public RegionFlag {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(codec, "codec");
    }

    public static <T> RegionFlag<T> of(Key key, RegionFlagCodec<T> codec, T defaultValue) {
        return new RegionFlag<>(key, codec, defaultValue);
    }

    public static RegionFlag<Boolean> bool(Key key, boolean defaultValue) {
        return of(key, RegionFlagCodec.bool(), defaultValue);
    }

    public static RegionFlag<Integer> integer(Key key, int defaultValue) {
        return of(key, RegionFlagCodec.integer(), defaultValue);
    }

    public static RegionFlag<String> string(Key key, String defaultValue) {
        return of(key, RegionFlagCodec.string(), defaultValue);
    }

    JsonElement encode(T value) {
        return codec.encode(value);
    }

    T decode(JsonElement json) {
        return codec.decode(json);
    }
}
