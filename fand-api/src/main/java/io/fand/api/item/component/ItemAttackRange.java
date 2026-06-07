package io.fand.api.item.component;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/** Typed value for {@code minecraft:attack_range}. */
public record ItemAttackRange(
        float minReach,
        float maxReach,
        float minCreativeReach,
        float maxCreativeReach,
        float hitboxMargin,
        float mobFactor) implements ItemComponentData {

    public static final ItemAttackRange DEFAULT = new ItemAttackRange(0.0F, 3.0F, 0.0F, 5.0F, 0.3F, 1.0F);

    public ItemAttackRange {
        range(minReach, 0.0F, 64.0F, "minReach");
        range(maxReach, 0.0F, 64.0F, "maxReach");
        range(minCreativeReach, 0.0F, 64.0F, "minCreativeReach");
        range(maxCreativeReach, 0.0F, 64.0F, "maxCreativeReach");
        range(hitboxMargin, 0.0F, 1.0F, "hitboxMargin");
        range(mobFactor, 0.0F, 2.0F, "mobFactor");
    }

    public static ItemAttackRange fromJson(JsonElement value) {
        if (value == null || !value.isJsonObject()) {
            return DEFAULT;
        }
        var object = value.getAsJsonObject();
        return new ItemAttackRange(
                object.has("min_reach") ? object.get("min_reach").getAsFloat() : 0.0F,
                object.has("max_reach") ? object.get("max_reach").getAsFloat() : 3.0F,
                object.has("min_creative_reach") ? object.get("min_creative_reach").getAsFloat() : 0.0F,
                object.has("max_creative_reach") ? object.get("max_creative_reach").getAsFloat() : 5.0F,
                object.has("hitbox_margin") ? object.get("hitbox_margin").getAsFloat() : 0.3F,
                object.has("mob_factor") ? object.get("mob_factor").getAsFloat() : 1.0F);
    }

    @Override
    public JsonObject toJson() {
        var json = new JsonObject();
        json.addProperty("min_reach", minReach);
        json.addProperty("max_reach", maxReach);
        json.addProperty("min_creative_reach", minCreativeReach);
        json.addProperty("max_creative_reach", maxCreativeReach);
        json.addProperty("hitbox_margin", hitboxMargin);
        json.addProperty("mob_factor", mobFactor);
        return json;
    }

    private static void range(float value, float min, float max, String name) {
        if (value < min || value > max) {
            throw new IllegalArgumentException(name + " must be in " + min + ".." + max);
        }
    }
}
