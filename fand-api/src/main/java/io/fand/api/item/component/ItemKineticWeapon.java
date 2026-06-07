package io.fand.api.item.component;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.Objects;
import java.util.Optional;
import net.kyori.adventure.key.Key;

/** Typed value for {@code minecraft:kinetic_weapon}. */
public record ItemKineticWeapon(
        int contactCooldownTicks,
        int delayTicks,
        Optional<ItemKineticWeapon.Condition> dismountConditions,
        Optional<ItemKineticWeapon.Condition> knockbackConditions,
        Optional<ItemKineticWeapon.Condition> damageConditions,
        float forwardMovement,
        float damageMultiplier,
        Optional<Key> sound,
        Optional<Key> hitSound) implements ItemComponentData {

    public ItemKineticWeapon {
        if (contactCooldownTicks < 0) {
            throw new IllegalArgumentException("contactCooldownTicks must be >= 0");
        }
        if (delayTicks < 0) {
            throw new IllegalArgumentException("delayTicks must be >= 0");
        }
        dismountConditions = Objects.requireNonNull(dismountConditions, "dismountConditions");
        knockbackConditions = Objects.requireNonNull(knockbackConditions, "knockbackConditions");
        damageConditions = Objects.requireNonNull(damageConditions, "damageConditions");
        sound = Objects.requireNonNull(sound, "sound");
        hitSound = Objects.requireNonNull(hitSound, "hitSound");
    }

    public static ItemKineticWeapon fromJson(JsonElement value) {
        var object = ItemComponentJson.objectOrEmpty(value);
        return new ItemKineticWeapon(
                ItemComponentJson.intOr(object, "contact_cooldown_ticks", 10),
                ItemComponentJson.intOr(object, "delay_ticks", 0),
                object.has("dismount_conditions") ? Optional.of(Condition.fromJson(object.get("dismount_conditions"))) : Optional.empty(),
                object.has("knockback_conditions") ? Optional.of(Condition.fromJson(object.get("knockback_conditions"))) : Optional.empty(),
                object.has("damage_conditions") ? Optional.of(Condition.fromJson(object.get("damage_conditions"))) : Optional.empty(),
                ItemComponentJson.floatOr(object, "forward_movement", 0.0F),
                ItemComponentJson.floatOr(object, "damage_multiplier", 1.0F),
                ItemComponentJson.optionalKey(object, "sound"),
                ItemComponentJson.optionalKey(object, "hit_sound"));
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
        dismountConditions.ifPresent(value -> json.add("dismount_conditions", value.toJson()));
        knockbackConditions.ifPresent(value -> json.add("knockback_conditions", value.toJson()));
        damageConditions.ifPresent(value -> json.add("damage_conditions", value.toJson()));
        if (forwardMovement != 0.0F) {
            json.addProperty("forward_movement", forwardMovement);
        }
        if (damageMultiplier != 1.0F) {
            json.addProperty("damage_multiplier", damageMultiplier);
        }
        sound.ifPresent(value -> json.addProperty("sound", value.asString()));
        hitSound.ifPresent(value -> json.addProperty("hit_sound", value.asString()));
        return json;
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
