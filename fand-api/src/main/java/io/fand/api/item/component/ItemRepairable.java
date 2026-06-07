package io.fand.api.item.component;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.Objects;

/** Typed value for {@code minecraft:repairable}. */
public record ItemRepairable(ItemKeySet items) implements ItemComponentData {

    public ItemRepairable {
        items = Objects.requireNonNull(items, "items");
    }

    public static ItemRepairable fromJson(JsonElement value) {
        var object = ItemComponentJson.object(value, "repairable");
        return new ItemRepairable(ItemKeySet.fromJson(object.get("items")));
    }

    @Override
    public JsonObject toJson() {
        var json = new JsonObject();
        json.add("items", items.toJson());
        return json;
    }
}
