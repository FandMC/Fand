package io.fand.api.item.component;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.Objects;
import net.kyori.adventure.key.Key;

/** Typed global position used by lodestone trackers. */
public record ItemGlobalPosition(Key dimension, ItemBlockPosition position) implements ItemComponentData {

    public ItemGlobalPosition {
        dimension = Objects.requireNonNull(dimension, "dimension");
        position = Objects.requireNonNull(position, "position");
    }

    public static ItemGlobalPosition fromJson(JsonElement value) {
        var object = ItemComponentJson.object(value, "global position");
        return new ItemGlobalPosition(
                Key.key(object.get("dimension").getAsString()),
                ItemBlockPosition.fromJson(object.get("pos")));
    }

    @Override
    public JsonObject toJson() {
        var json = new JsonObject();
        json.addProperty("dimension", dimension.asString());
        json.add("pos", position.toJson());
        return json;
    }
}
