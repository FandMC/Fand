package io.fand.api.item.component;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/** Typed value for {@code minecraft:weapon}. */
public record ItemWeapon(int itemDamagePerAttack, float disableBlockingForSeconds) implements ItemComponentData {

    public ItemWeapon {
        if (itemDamagePerAttack < 0) {
            throw new IllegalArgumentException("itemDamagePerAttack must be >= 0");
        }
        if (disableBlockingForSeconds < 0.0F) {
            throw new IllegalArgumentException("disableBlockingForSeconds must be >= 0");
        }
    }

    public ItemWeapon(int itemDamagePerAttack) {
        this(itemDamagePerAttack, 0.0F);
    }

    public static ItemWeapon fromJson(JsonElement value) {
        if (value == null || !value.isJsonObject()) {
            return new ItemWeapon(1);
        }
        var object = value.getAsJsonObject();
        return new ItemWeapon(
                object.has("item_damage_per_attack") ? object.get("item_damage_per_attack").getAsInt() : 1,
                object.has("disable_blocking_for_seconds") ? object.get("disable_blocking_for_seconds").getAsFloat() : 0.0F);
    }

    @Override
    public JsonObject toJson() {
        var json = new JsonObject();
        json.addProperty("item_damage_per_attack", itemDamagePerAttack);
        json.addProperty("disable_blocking_for_seconds", disableBlockingForSeconds);
        return json;
    }
}
