package io.fand.api.item.component;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.List;
import java.util.Objects;

/** Typed value for {@code minecraft:death_protection}. */
public record ItemDeathProtection(List<ItemConsumeEffect> deathEffects) implements ItemComponentData {

    public static final ItemDeathProtection EMPTY = new ItemDeathProtection(List.of());

    public ItemDeathProtection {
        deathEffects = List.copyOf(Objects.requireNonNull(deathEffects, "deathEffects"));
    }

    public static ItemDeathProtection fromJson(JsonElement value) {
        var object = ItemComponentJson.objectOrEmpty(value);
        var effects = new java.util.ArrayList<ItemConsumeEffect>();
        var rawEffects = object.get("death_effects");
        if (rawEffects != null && rawEffects.isJsonArray()) {
            for (var effect : rawEffects.getAsJsonArray()) {
                effects.add(ItemConsumeEffect.fromJson(effect));
            }
        }
        return new ItemDeathProtection(effects);
    }

    @Override
    public JsonObject toJson() {
        var json = new JsonObject();
        if (!deathEffects.isEmpty()) {
            var effects = new JsonArray();
            deathEffects.forEach(effect -> effects.add(effect.toJson()));
            json.add("death_effects", effects);
        }
        return json;
    }
}
