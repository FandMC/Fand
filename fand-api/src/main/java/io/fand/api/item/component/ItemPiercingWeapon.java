package io.fand.api.item.component;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.Objects;
import java.util.Optional;
import net.kyori.adventure.key.Key;
import org.jspecify.annotations.Nullable;

/** Typed value for {@code minecraft:piercing_weapon}. */
public final class ItemPiercingWeapon implements ItemComponentData {

    private final boolean dealsKnockback;
    private final boolean dismounts;
    private final @Nullable Key sound;
    private final @Nullable Key hitSound;

    public ItemPiercingWeapon(boolean dealsKnockback, boolean dismounts, @Nullable Key sound, @Nullable Key hitSound) {
        this.dealsKnockback = dealsKnockback;
        this.dismounts = dismounts;
        this.sound = sound;
        this.hitSound = hitSound;
    }

    public ItemPiercingWeapon() {
        this(true, false, null, null);
    }

    public static ItemPiercingWeapon fromJson(JsonElement value) {
        var object = ItemComponentJson.objectOrEmpty(value);
        return new ItemPiercingWeapon(
                ItemComponentJson.booleanOr(object, "deals_knockback", true),
                ItemComponentJson.booleanOr(object, "dismounts", false),
                ItemComponentJson.optionalKey(object, "sound").orElse(null),
                ItemComponentJson.optionalKey(object, "hit_sound").orElse(null));
    }

    public boolean dealsKnockback() {
        return dealsKnockback;
    }

    public boolean dismounts() {
        return dismounts;
    }

    public Optional<Key> sound() {
        return Optional.ofNullable(sound);
    }

    public Optional<Key> hitSound() {
        return Optional.ofNullable(hitSound);
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
        if (sound != null) {
            json.addProperty("sound", sound.asString());
        }
        if (hitSound != null) {
            json.addProperty("hit_sound", hitSound.asString());
        }
        return json;
    }

    @Override
    public boolean equals(@Nullable Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof ItemPiercingWeapon that)) {
            return false;
        }
        return dealsKnockback == that.dealsKnockback
                && dismounts == that.dismounts
                && Objects.equals(sound, that.sound)
                && Objects.equals(hitSound, that.hitSound);
    }

    @Override
    public int hashCode() {
        return Objects.hash(dealsKnockback, dismounts, sound, hitSound);
    }

    @Override
    public String toString() {
        return "ItemPiercingWeapon[dealsKnockback=" + dealsKnockback
                + ", dismounts=" + dismounts
                + ", sound=" + sound()
                + ", hitSound=" + hitSound()
                + "]";
    }
}
