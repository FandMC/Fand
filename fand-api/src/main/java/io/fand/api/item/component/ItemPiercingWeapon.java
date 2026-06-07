package io.fand.api.item.component;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.Objects;
import java.util.Optional;
import net.kyori.adventure.key.Key;

/** Typed value for {@code minecraft:piercing_weapon}. */
public record ItemPiercingWeapon(
        boolean dealsKnockback,
        boolean dismounts,
        Optional<Key> sound,
        Optional<Key> hitSound) implements ItemComponentData {

    public ItemPiercingWeapon {
        sound = Objects.requireNonNull(sound, "sound");
        hitSound = Objects.requireNonNull(hitSound, "hitSound");
    }

    public ItemPiercingWeapon() {
        this(true, false, Optional.empty(), Optional.empty());
    }

    public static ItemPiercingWeapon fromJson(JsonElement value) {
        var object = ItemComponentJson.objectOrEmpty(value);
        return new ItemPiercingWeapon(
                ItemComponentJson.booleanOr(object, "deals_knockback", true),
                ItemComponentJson.booleanOr(object, "dismounts", false),
                ItemComponentJson.optionalKey(object, "sound"),
                ItemComponentJson.optionalKey(object, "hit_sound"));
    }

    @Override
    public JsonObject toJson() {
        var json = new JsonObject();
        if (!dealsKnockback) {
            json.addProperty("deals_knockback", false);
        }
        if (dismounts) {
            json.addProperty("dismounts", true);
        }
        sound.ifPresent(value -> json.addProperty("sound", value.asString()));
        hitSound.ifPresent(value -> json.addProperty("hit_sound", value.asString()));
        return json;
    }
}
