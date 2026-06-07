package io.fand.api.item.component;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/** Typed value for {@code minecraft:block_state}. */
public record ItemBlockStateProperties(Map<String, String> properties) implements ItemComponentData {

    public static final ItemBlockStateProperties EMPTY = new ItemBlockStateProperties(Map.of());

    public ItemBlockStateProperties {
        var copied = new LinkedHashMap<String, String>();
        Objects.requireNonNull(properties, "properties")
                .forEach((name, value) -> copied.put(Objects.requireNonNull(name, "name"), Objects.requireNonNull(value, "value")));
        properties = Collections.unmodifiableMap(copied);
    }

    public ItemBlockStateProperties with(String name, String value) {
        var next = new LinkedHashMap<>(properties);
        next.put(name, value);
        return new ItemBlockStateProperties(next);
    }

    public static ItemBlockStateProperties fromJson(JsonElement value) {
        if (value == null || !value.isJsonObject()) {
            return EMPTY;
        }
        var properties = new LinkedHashMap<String, String>();
        value.getAsJsonObject().entrySet().forEach(entry -> {
            if (entry.getValue().isJsonPrimitive()) {
                properties.put(entry.getKey(), entry.getValue().getAsString());
            }
        });
        return new ItemBlockStateProperties(properties);
    }

    @Override
    public JsonObject toJson() {
        var json = new JsonObject();
        properties.forEach(json::addProperty);
        return json;
    }
}
