package io.fand.api.item.component;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import net.kyori.adventure.key.Key;

/** Typed value for {@code minecraft:map_decorations}. */
public record ItemMapDecorations(Map<String, ItemMapDecorations.Entry> decorations) implements ItemComponentData {

    public static final ItemMapDecorations EMPTY = new ItemMapDecorations(Map.of());

    public ItemMapDecorations {
        var copied = new LinkedHashMap<String, Entry>();
        Objects.requireNonNull(decorations, "decorations")
                .forEach((id, entry) -> copied.put(Objects.requireNonNull(id, "id"), Objects.requireNonNull(entry, "entry")));
        decorations = Collections.unmodifiableMap(copied);
    }

    public ItemMapDecorations with(String id, Entry entry) {
        var next = new LinkedHashMap<>(decorations);
        next.put(id, entry);
        return new ItemMapDecorations(next);
    }

    public static ItemMapDecorations fromJson(JsonElement value) {
        if (value == null || !value.isJsonObject()) {
            return EMPTY;
        }
        var entries = new LinkedHashMap<String, Entry>();
        for (var entry : value.getAsJsonObject().entrySet()) {
            entries.put(entry.getKey(), Entry.fromJson(entry.getValue()));
        }
        return new ItemMapDecorations(entries);
    }

    @Override
    public JsonObject toJson() {
        var json = new JsonObject();
        decorations.forEach((id, entry) -> json.add(id, entry.toJson()));
        return json;
    }

    public record Entry(Key type, double x, double z, float rotation) implements ItemComponentData {

        public Entry {
            type = Objects.requireNonNull(type, "type");
        }

        public static Entry fromJson(JsonElement value) {
            var object = ItemComponentJson.object(value, "map decoration");
            return new Entry(
                    ItemComponentJson.key(object, "type"),
                    object.get("x").getAsDouble(),
                    object.get("z").getAsDouble(),
                    object.get("rotation").getAsFloat());
        }

        @Override
        public JsonObject toJson() {
            var json = new JsonObject();
            json.addProperty("type", type.asString());
            json.addProperty("x", x);
            json.addProperty("z", z);
            json.addProperty("rotation", rotation);
            return json;
        }
    }
}
