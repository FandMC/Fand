package io.fand.api.advancement;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import java.util.Objects;
import java.util.Optional;

/**
 * Vanilla advancement numeric range. A point is encoded as a primitive; bounded
 * ranges are encoded as {@code {"min": ..., "max": ...}}.
 */
public record AdvancementRange(Optional<Double> min, Optional<Double> max) {

    public static final AdvancementRange ANY = new AdvancementRange(Optional.empty(), Optional.empty());

    public AdvancementRange {
        min = Objects.requireNonNull(min, "min");
        max = Objects.requireNonNull(max, "max");
        if (min.isPresent() && max.isPresent() && min.get() > max.get()) {
            throw new IllegalArgumentException("range min must be <= max");
        }
    }

    public static AdvancementRange exactly(double value) {
        return new AdvancementRange(Optional.of(value), Optional.of(value));
    }

    public static AdvancementRange between(double min, double max) {
        return new AdvancementRange(Optional.of(min), Optional.of(max));
    }

    public static AdvancementRange atLeast(double min) {
        return new AdvancementRange(Optional.of(min), Optional.empty());
    }

    public static AdvancementRange atMost(double max) {
        return new AdvancementRange(Optional.empty(), Optional.of(max));
    }

    public boolean any() {
        return min.isEmpty() && max.isEmpty();
    }

    public JsonElement toJson() {
        if (min.isPresent() && min.equals(max)) {
            return new JsonPrimitive(min.get());
        }
        var json = new JsonObject();
        min.ifPresent(value -> json.addProperty("min", value));
        max.ifPresent(value -> json.addProperty("max", value));
        return json;
    }
}
