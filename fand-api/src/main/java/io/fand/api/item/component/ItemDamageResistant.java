package io.fand.api.item.component;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.Objects;

/** Typed value for {@code minecraft:damage_resistant}. */
public record ItemDamageResistant(ItemKeySet types) implements ItemComponentData {

    public ItemDamageResistant {
        types = Objects.requireNonNull(types, "types");
    }

    public static ItemDamageResistant fromJson(JsonElement value) {
        var object = ItemComponentJson.object(value, "damage resistant");
        return new ItemDamageResistant(ItemKeySet.fromJson(object.get("types")));
    }

    @Override
    public JsonObject toJson() {
        var json = new JsonObject();
        json.add("types", types.toJson());
        return json;
    }
}
