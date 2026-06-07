package io.fand.api.item.component;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/** Typed value for {@code minecraft:use_effects}. */
public record ItemUseEffects(boolean canSprint, boolean interactVibrations, float speedMultiplier) implements ItemComponentData {

    public static final ItemUseEffects DEFAULT = new ItemUseEffects(false, true, 0.2F);

    public ItemUseEffects {
        if (speedMultiplier < 0.0F || speedMultiplier > 1.0F) {
            throw new IllegalArgumentException("speedMultiplier must be in 0.0..1.0");
        }
    }

    public static ItemUseEffects fromJson(JsonElement value) {
        if (value == null || !value.isJsonObject()) {
            return DEFAULT;
        }
        var object = value.getAsJsonObject();
        return new ItemUseEffects(
                object.has("can_sprint") && object.get("can_sprint").getAsBoolean(),
                !object.has("interact_vibrations") || object.get("interact_vibrations").getAsBoolean(),
                object.has("speed_multiplier") ? object.get("speed_multiplier").getAsFloat() : 0.2F);
    }

    @Override
    public JsonObject toJson() {
        var json = new JsonObject();
        json.addProperty("can_sprint", canSprint);
        json.addProperty("interact_vibrations", interactVibrations);
        json.addProperty("speed_multiplier", speedMultiplier);
        return json;
    }
}
