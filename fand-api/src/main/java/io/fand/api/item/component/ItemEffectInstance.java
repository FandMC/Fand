package io.fand.api.item.component;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.Objects;
import java.util.Optional;
import net.kyori.adventure.key.Key;

/** Typed mob-effect instance used by potion and consume-effect components. */
public record ItemEffectInstance(
        Key effect,
        int duration,
        int amplifier,
        boolean ambient,
        boolean showParticles,
        boolean showIcon,
        Optional<ItemEffectInstance> hiddenEffect) implements ItemComponentData {

    public ItemEffectInstance {
        effect = Objects.requireNonNull(effect, "effect");
        hiddenEffect = Objects.requireNonNull(hiddenEffect, "hiddenEffect");
        if (amplifier < 0 || amplifier > 255) {
            throw new IllegalArgumentException("amplifier must be in 0..255");
        }
    }

    public ItemEffectInstance(Key effect, int duration) {
        this(effect, duration, 0, false, true, true, Optional.empty());
    }

    public ItemEffectInstance(EffectKey effect, int duration) {
        this(Objects.requireNonNull(effect, "effect").key(), duration);
    }

    public ItemEffectInstance(
            EffectKey effect,
            int duration,
            int amplifier,
            boolean ambient,
            boolean showParticles,
            boolean showIcon,
            Optional<ItemEffectInstance> hiddenEffect) {
        this(Objects.requireNonNull(effect, "effect").key(), duration, amplifier, ambient, showParticles, showIcon, hiddenEffect);
    }

    public static ItemEffectInstance fromJson(JsonElement value) {
        var object = ItemComponentJson.object(value, "effect instance");
        boolean showParticles = ItemComponentJson.booleanOr(object, "show_particles", true);
        return new ItemEffectInstance(
                ItemComponentJson.key(object, "id"),
                ItemComponentJson.intOr(object, "duration", 0),
                ItemComponentJson.intOr(object, "amplifier", 0),
                ItemComponentJson.booleanOr(object, "ambient", false),
                showParticles,
                ItemComponentJson.booleanOr(object, "show_icon", showParticles),
                object.has("hidden_effect")
                        ? Optional.of(ItemEffectInstance.fromJson(object.get("hidden_effect")))
                        : Optional.empty());
    }

    @Override
    public JsonObject toJson() {
        var json = new JsonObject();
        json.addProperty("id", effect.asString());
        if (amplifier != 0) {
            json.addProperty("amplifier", amplifier);
        }
        if (duration != 0) {
            json.addProperty("duration", duration);
        }
        if (ambient) {
            json.addProperty("ambient", true);
        }
        if (!showParticles) {
            json.addProperty("show_particles", false);
        }
        if (showIcon != showParticles) {
            json.addProperty("show_icon", showIcon);
        }
        hiddenEffect.ifPresent(effect -> json.add("hidden_effect", effect.toJson()));
        return json;
    }
}
