package io.fand.api.item.component;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.Objects;
import java.util.Optional;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;

/** Filterable Adventure component value used by written books. */
public record ItemFilterableComponent(Component raw, Optional<Component> filtered) implements ItemComponentData {

    public ItemFilterableComponent {
        raw = Objects.requireNonNull(raw, "raw");
        filtered = Objects.requireNonNull(filtered, "filtered");
    }

    public static ItemFilterableComponent of(Component raw) {
        return new ItemFilterableComponent(raw, Optional.empty());
    }

    public static ItemFilterableComponent fromJson(JsonElement value) {
        Objects.requireNonNull(value, "value");
        if (!value.isJsonObject() || !value.getAsJsonObject().has("raw")) {
            return of(deserialize(value));
        }
        var object = value.getAsJsonObject();
        return new ItemFilterableComponent(
                deserialize(object.get("raw")),
                object.has("filtered") ? Optional.of(deserialize(object.get("filtered"))) : Optional.empty());
    }

    @Override
    public JsonElement toJson() {
        if (filtered.isEmpty()) {
            return serialize(raw);
        }
        var json = new JsonObject();
        json.add("raw", serialize(raw));
        json.add("filtered", serialize(filtered.orElseThrow()));
        return json;
    }

    private static JsonElement serialize(Component component) {
        return GsonComponentSerializer.gson().serializeToTree(component);
    }

    private static Component deserialize(JsonElement component) {
        return GsonComponentSerializer.gson().deserializeFromTree(component);
    }
}
