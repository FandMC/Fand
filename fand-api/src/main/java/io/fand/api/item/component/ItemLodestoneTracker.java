package io.fand.api.item.component;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.Objects;
import java.util.Optional;

/** Typed value for {@code minecraft:lodestone_tracker}. */
public record ItemLodestoneTracker(Optional<ItemGlobalPosition> target, boolean tracked) implements ItemComponentData {

    public ItemLodestoneTracker {
        target = Objects.requireNonNull(target, "target");
    }

    public static ItemLodestoneTracker fromJson(JsonElement value) {
        var object = ItemComponentJson.objectOrEmpty(value);
        return new ItemLodestoneTracker(
                object.has("target") ? Optional.of(ItemGlobalPosition.fromJson(object.get("target"))) : Optional.empty(),
                ItemComponentJson.booleanOr(object, "tracked", true));
    }

    @Override
    public JsonObject toJson() {
        var json = new JsonObject();
        target.ifPresent(value -> json.add("target", value.toJson()));
        if (!tracked) {
            json.addProperty("tracked", false);
        }
        return json;
    }
}
