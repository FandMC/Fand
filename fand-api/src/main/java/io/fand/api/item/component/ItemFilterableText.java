package io.fand.api.item.component;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import java.util.Objects;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/** Filterable string value used by writable and written books. */
public final class ItemFilterableText implements ItemComponentData {

    private final String raw;
    private final @Nullable String filtered;

    public ItemFilterableText(String raw, @Nullable String filtered) {
        this.raw = Objects.requireNonNull(raw, "raw");
        this.filtered = filtered;
    }

    public static ItemFilterableText of(String raw) {
        return new ItemFilterableText(raw, null);
    }

    public static ItemFilterableText fromJson(JsonElement value) {
        Objects.requireNonNull(value, "value");
        if (value.isJsonPrimitive()) {
            return of(value.getAsString());
        }
        var object = ItemComponentJson.object(value, "filterable text");
        return new ItemFilterableText(
                object.get("raw").getAsString(),
                ItemComponentJson.optionalString(object, "filtered").orElse(null));
    }

    public String raw() {
        return raw;
    }

    public Optional<String> filtered() {
        return Optional.ofNullable(filtered);
    }

    @Override
    public JsonElement toJson() {
        if (filtered == null) {
            return new JsonPrimitive(raw);
        }
        var json = new JsonObject();
        json.addProperty("raw", raw);
        json.addProperty("filtered", filtered);
        return json;
    }

    @Override
    public boolean equals(@Nullable Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof ItemFilterableText that)) {
            return false;
        }
        return raw.equals(that.raw) && Objects.equals(filtered, that.filtered);
    }

    @Override
    public int hashCode() {
        return Objects.hash(raw, filtered);
    }

    @Override
    public String toString() {
        return "ItemFilterableText[raw=" + raw + ", filtered=" + filtered() + "]";
    }
}
