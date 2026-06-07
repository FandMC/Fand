package io.fand.api.item.component;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import java.util.Objects;
import java.util.Optional;

/** Filterable string value used by writable and written books. */
public record ItemFilterableText(String raw, Optional<String> filtered) implements ItemComponentData {

    public ItemFilterableText {
        raw = Objects.requireNonNull(raw, "raw");
        filtered = Objects.requireNonNull(filtered, "filtered");
    }

    public static ItemFilterableText of(String raw) {
        return new ItemFilterableText(raw, Optional.empty());
    }

    public static ItemFilterableText fromJson(JsonElement value) {
        Objects.requireNonNull(value, "value");
        if (value.isJsonPrimitive()) {
            return of(value.getAsString());
        }
        var object = ItemComponentJson.object(value, "filterable text");
        return new ItemFilterableText(
                object.get("raw").getAsString(),
                ItemComponentJson.optionalString(object, "filtered"));
    }

    @Override
    public JsonElement toJson() {
        if (filtered.isEmpty()) {
            return new JsonPrimitive(raw);
        }
        var json = new JsonObject();
        json.addProperty("raw", raw);
        json.addProperty("filtered", filtered.orElseThrow());
        return json;
    }
}
