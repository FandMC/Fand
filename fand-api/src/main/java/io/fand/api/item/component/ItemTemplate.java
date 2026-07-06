package io.fand.api.item.component;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import io.fand.api.item.ItemKey;
import java.util.Objects;
import net.kyori.adventure.key.Key;

/** Immutable API value for vanilla {@code ItemStackTemplate}. */
public record ItemTemplate(Key item, int count, ItemComponents components) implements ItemComponentData {

    public ItemTemplate {
        item = Objects.requireNonNull(item, "item");
        components = components == null ? ItemComponents.EMPTY : components;
        if (count < 1 || count > 99) {
            throw new IllegalArgumentException("count must be in 1..99");
        }
    }

    public ItemTemplate(Key item) {
        this(item, 1, ItemComponents.EMPTY);
    }

    public ItemTemplate(ItemKey item) {
        this(Objects.requireNonNull(item, "item").key());
    }

    public ItemTemplate(ItemKey item, int count, ItemComponents components) {
        this(Objects.requireNonNull(item, "item").key(), count, components);
    }

    public static ItemTemplate of(Key item) {
        return new ItemTemplate(item);
    }

    public static ItemTemplate of(ItemKey item) {
        return new ItemTemplate(item);
    }

    public static ItemTemplate fromJson(JsonElement value) {
        Objects.requireNonNull(value, "value");
        if (value.isJsonPrimitive()) {
            return new ItemTemplate(Key.key(value.getAsString()));
        }
        if (!value.isJsonObject()) {
            throw new IllegalArgumentException("Item template must be a string or JSON object");
        }
        var object = value.getAsJsonObject();
        if (!object.has("id") || !object.get("id").isJsonPrimitive()) {
            throw new IllegalArgumentException("Item template must contain an id");
        }
        int count = object.has("count") ? object.get("count").getAsInt() : 1;
        ItemComponents components = object.has("components")
                ? ItemComponents.fromJsonPatch(object.get("components"))
                : ItemComponents.EMPTY;
        return new ItemTemplate(Key.key(object.get("id").getAsString()), count, components);
    }

    public ItemTemplate withCount(int count) {
        return new ItemTemplate(item, count, components);
    }

    public ItemTemplate withComponents(ItemComponents components) {
        return new ItemTemplate(item, count, components);
    }

    @Override
    public JsonElement toJson() {
        if (count == 1 && components.empty()) {
            return new JsonPrimitive(item.asString());
        }
        var json = new JsonObject();
        json.addProperty("id", item.asString());
        if (count != 1) {
            json.addProperty("count", count);
        }
        if (!components.empty()) {
            json.add("components", components.toJsonPatch());
        }
        return json;
    }
}
