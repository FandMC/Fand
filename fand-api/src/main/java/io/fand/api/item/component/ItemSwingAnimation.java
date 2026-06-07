package io.fand.api.item.component;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.Objects;

/** Typed value for {@code minecraft:swing_animation}. */
public record ItemSwingAnimation(ItemSwingAnimationType type, int duration) implements ItemComponentData {

    public static final ItemSwingAnimation DEFAULT = new ItemSwingAnimation(ItemSwingAnimationType.WHACK, 6);

    public ItemSwingAnimation {
        type = Objects.requireNonNull(type, "type");
        if (duration <= 0) {
            throw new IllegalArgumentException("duration must be > 0");
        }
    }

    public static ItemSwingAnimation fromJson(JsonElement value) {
        if (value == null || !value.isJsonObject()) {
            return DEFAULT;
        }
        var object = value.getAsJsonObject();
        return new ItemSwingAnimation(
                object.has("type") ? ItemSwingAnimationType.fromSerializedName(object.get("type").getAsString()) : ItemSwingAnimationType.WHACK,
                object.has("duration") ? object.get("duration").getAsInt() : 6);
    }

    @Override
    public JsonObject toJson() {
        var json = new JsonObject();
        json.addProperty("type", type.serializedName());
        json.addProperty("duration", duration);
        return json;
    }
}
