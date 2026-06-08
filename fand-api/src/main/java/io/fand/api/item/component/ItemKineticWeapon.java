package io.fand.api.item.component;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.Objects;
import java.util.Optional;
import net.kyori.adventure.key.Key;
import org.jspecify.annotations.Nullable;

/** Typed value for {@code minecraft:kinetic_weapon}. */
public final class ItemKineticWeapon implements ItemComponentData {

    private final int contactCooldownTicks;
    private final int delayTicks;
    private final @Nullable Condition dismountConditions;
    private final @Nullable Condition knockbackConditions;
    private final @Nullable Condition damageConditions;
    private final float forwardMovement;
    private final float damageMultiplier;
    private final @Nullable Key sound;
    private final @Nullable Key hitSound;

    public ItemKineticWeapon(
            int contactCooldownTicks,
            int delayTicks,
            @Nullable Condition dismountConditions,
            @Nullable Condition knockbackConditions,
            @Nullable Condition damageConditions,
            float forwardMovement,
            float damageMultiplier,
            @Nullable Key sound,
            @Nullable Key hitSound) {
        if (contactCooldownTicks < 0) {
            throw new IllegalArgumentException("contactCooldownTicks must be >= 0");
        }
        if (delayTicks < 0) {
            throw new IllegalArgumentException("delayTicks must be >= 0");
        }
        this.contactCooldownTicks = contactCooldownTicks;
        this.delayTicks = delayTicks;
        this.dismountConditions = dismountConditions;
        this.knockbackConditions = knockbackConditions;
        this.damageConditions = damageConditions;
        this.forwardMovement = forwardMovement;
        this.damageMultiplier = damageMultiplier;
        this.sound = sound;
        this.hitSound = hitSound;
    }

    public static ItemKineticWeapon fromJson(JsonElement value) {
        var object = ItemComponentJson.objectOrEmpty(value);
        return new ItemKineticWeapon(
                ItemComponentJson.intOr(object, "contact_cooldown_ticks", 10),
                ItemComponentJson.intOr(object, "delay_ticks", 0),
                object.has("dismount_conditions") ? Condition.fromJson(object.get("dismount_conditions")) : null,
                object.has("knockback_conditions") ? Condition.fromJson(object.get("knockback_conditions")) : null,
                object.has("damage_conditions") ? Condition.fromJson(object.get("damage_conditions")) : null,
                ItemComponentJson.floatOr(object, "forward_movement", 0.0F),
                ItemComponentJson.floatOr(object, "damage_multiplier", 1.0F),
                ItemComponentJson.optionalKey(object, "sound").orElse(null),
                ItemComponentJson.optionalKey(object, "hit_sound").orElse(null));
    }

    public int contactCooldownTicks() {
        return contactCooldownTicks;
    }

    public int delayTicks() {
        return delayTicks;
    }

    public Optional<Condition> dismountConditions() {
        return Optional.ofNullable(dismountConditions);
    }

    public Optional<Condition> knockbackConditions() {
        return Optional.ofNullable(knockbackConditions);
    }

    public Optional<Condition> damageConditions() {
        return Optional.ofNullable(damageConditions);
    }

    public float forwardMovement() {
        return forwardMovement;
    }

    public float damageMultiplier() {
        return damageMultiplier;
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
        if (contactCooldownTicks != 10) {
            json.addProperty("contact_cooldown_ticks", contactCooldownTicks);
        }
        if (delayTicks != 0) {
            json.addProperty("delay_ticks", delayTicks);
        }
        if (dismountConditions != null) {
            json.add("dismount_conditions", dismountConditions.toJson());
        }
        if (knockbackConditions != null) {
            json.add("knockback_conditions", knockbackConditions.toJson());
        }
        if (damageConditions != null) {
            json.add("damage_conditions", damageConditions.toJson());
        }
        if (forwardMovement != 0.0F) {
            json.addProperty("forward_movement", forwardMovement);
        }
        if (damageMultiplier != 1.0F) {
            json.addProperty("damage_multiplier", damageMultiplier);
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
        if (!(other instanceof ItemKineticWeapon that)) {
            return false;
        }
        return contactCooldownTicks == that.contactCooldownTicks
                && delayTicks == that.delayTicks
                && Float.compare(forwardMovement, that.forwardMovement) == 0
                && Float.compare(damageMultiplier, that.damageMultiplier) == 0
                && Objects.equals(dismountConditions, that.dismountConditions)
                && Objects.equals(knockbackConditions, that.knockbackConditions)
                && Objects.equals(damageConditions, that.damageConditions)
                && Objects.equals(sound, that.sound)
                && Objects.equals(hitSound, that.hitSound);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                contactCooldownTicks,
                delayTicks,
                dismountConditions,
                knockbackConditions,
                damageConditions,
                forwardMovement,
                damageMultiplier,
                sound,
                hitSound);
    }

    @Override
    public String toString() {
        return "ItemKineticWeapon[contactCooldownTicks=" + contactCooldownTicks
                + ", delayTicks=" + delayTicks
                + ", dismountConditions=" + dismountConditions()
                + ", knockbackConditions=" + knockbackConditions()
                + ", damageConditions=" + damageConditions()
                + ", forwardMovement=" + forwardMovement
                + ", damageMultiplier=" + damageMultiplier
                + ", sound=" + sound()
                + ", hitSound=" + hitSound()
                + "]";
    }

    public record Condition(int maxDurationTicks, float minSpeed, float minRelativeSpeed) implements ItemComponentData {

        public Condition {
            if (maxDurationTicks < 0) {
                throw new IllegalArgumentException("maxDurationTicks must be >= 0");
            }
        }

        public static Condition fromJson(JsonElement value) {
            var object = ItemComponentJson.object(value, "kinetic weapon condition");
            return new Condition(
                    object.get("max_duration_ticks").getAsInt(),
                    ItemComponentJson.floatOr(object, "min_speed", 0.0F),
                    ItemComponentJson.floatOr(object, "min_relative_speed", 0.0F));
        }

        @Override
        public JsonObject toJson() {
            var json = new JsonObject();
            json.addProperty("max_duration_ticks", maxDurationTicks);
            if (minSpeed != 0.0F) {
                json.addProperty("min_speed", minSpeed);
            }
            if (minRelativeSpeed != 0.0F) {
                json.addProperty("min_relative_speed", minRelativeSpeed);
            }
            return json;
        }
    }
}
