package io.fand.api.item.component;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/** Typed value for {@code minecraft:food}. */
public record ItemFood(int nutrition, float saturation, boolean canAlwaysEat) implements ItemComponentData {

    public ItemFood {
        if (nutrition < 0) {
            throw new IllegalArgumentException("nutrition must be >= 0");
        }
    }

    public static ItemFood fromJson(JsonElement value) {
        if (value == null || !value.isJsonObject()) {
            throw new IllegalArgumentException("food must be a JSON object");
        }
        var object = value.getAsJsonObject();
        return new ItemFood(
                object.get("nutrition").getAsInt(),
                object.get("saturation").getAsFloat(),
                object.has("can_always_eat") && object.get("can_always_eat").getAsBoolean());
    }

    @Override
    public JsonObject toJson() {
        var json = new JsonObject();
        json.addProperty("nutrition", nutrition);
        json.addProperty("saturation", saturation);
        if (canAlwaysEat) {
            json.addProperty("can_always_eat", true);
        }
        return json;
    }
}
