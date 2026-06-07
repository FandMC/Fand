package io.fand.api.item.component;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import net.kyori.adventure.key.Key;

/** Typed value for {@code minecraft:debug_stick_state}. */
public record ItemDebugStickState(Map<Key, String> properties) implements ItemComponentData {

    public static final ItemDebugStickState EMPTY = new ItemDebugStickState(Map.of());

    public ItemDebugStickState {
        var copied = new LinkedHashMap<Key, String>();
        Objects.requireNonNull(properties, "properties")
                .forEach((block, property) -> copied.put(Objects.requireNonNull(block, "block"), Objects.requireNonNull(property, "property")));
        properties = Collections.unmodifiableMap(copied);
    }

    public ItemDebugStickState with(Key block, String property) {
        var next = new LinkedHashMap<>(properties);
        next.put(block, property);
        return new ItemDebugStickState(next);
    }

    public static ItemDebugStickState fromJson(JsonElement value) {
        if (value == null || !value.isJsonObject()) {
            return EMPTY;
        }
        var properties = new LinkedHashMap<Key, String>();
        for (var entry : value.getAsJsonObject().entrySet()) {
            properties.put(Key.key(entry.getKey()), entry.getValue().getAsString());
        }
        return new ItemDebugStickState(properties);
    }

    @Override
    public JsonObject toJson() {
        var json = new JsonObject();
        properties.forEach((block, property) -> json.addProperty(block.asString(), property));
        return json;
    }
}
