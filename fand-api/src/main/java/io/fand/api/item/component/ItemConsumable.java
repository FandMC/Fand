package io.fand.api.item.component;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.fand.api.world.sound.SoundKey;
import java.util.List;
import java.util.Objects;
import net.kyori.adventure.key.Key;

/** Typed value for {@code minecraft:consumable}. */
public record ItemConsumable(
        float consumeSeconds,
        ItemUseAnimation animation,
        Key sound,
        boolean hasConsumeParticles,
        List<ItemConsumeEffect> onConsumeEffects) implements ItemComponentData {

    public static final Key DEFAULT_SOUND = Key.key("minecraft:entity.generic.eat");
    public static final ItemConsumable DEFAULT =
            new ItemConsumable(1.6F, ItemUseAnimation.EAT, DEFAULT_SOUND, true, List.of());

    public ItemConsumable {
        if (consumeSeconds < 0.0F) {
            throw new IllegalArgumentException("consumeSeconds must be >= 0");
        }
        animation = Objects.requireNonNull(animation, "animation");
        sound = Objects.requireNonNull(sound, "sound");
        onConsumeEffects = List.copyOf(Objects.requireNonNull(onConsumeEffects, "onConsumeEffects"));
    }

    public ItemConsumable(
            float consumeSeconds,
            ItemUseAnimation animation,
            SoundKey sound,
            boolean hasConsumeParticles,
            List<ItemConsumeEffect> onConsumeEffects) {
        this(consumeSeconds, animation, Objects.requireNonNull(sound, "sound").key(), hasConsumeParticles, onConsumeEffects);
    }

    public static ItemConsumable fromJson(JsonElement value) {
        if (value == null || !value.isJsonObject()) {
            return DEFAULT;
        }
        var object = value.getAsJsonObject();
        var effects = new java.util.ArrayList<ItemConsumeEffect>();
        var rawEffects = object.get("on_consume_effects");
        if (rawEffects != null && rawEffects.isJsonArray()) {
            for (var effect : rawEffects.getAsJsonArray()) {
                effects.add(ItemConsumeEffect.fromJson(effect));
            }
        }
        return new ItemConsumable(
                object.has("consume_seconds") ? object.get("consume_seconds").getAsFloat() : 1.6F,
                object.has("animation") ? ItemUseAnimation.fromSerializedName(object.get("animation").getAsString()) : ItemUseAnimation.EAT,
                object.has("sound") ? Key.key(object.get("sound").getAsString()) : DEFAULT_SOUND,
                !object.has("has_consume_particles") || object.get("has_consume_particles").getAsBoolean(),
                effects);
    }

    @Override
    public JsonObject toJson() {
        var json = new JsonObject();
        json.addProperty("consume_seconds", consumeSeconds);
        json.addProperty("animation", animation.serializedName());
        json.addProperty("sound", sound.asString());
        json.addProperty("has_consume_particles", hasConsumeParticles);
        if (!onConsumeEffects.isEmpty()) {
            var effects = new JsonArray();
            onConsumeEffects.forEach(effect -> effects.add(effect.toJson()));
            json.add("on_consume_effects", effects);
        }
        return json;
    }
}
