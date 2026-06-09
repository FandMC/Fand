package io.fand.api.item.component;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.Objects;
import java.util.Optional;
import net.kyori.adventure.key.Key;
import org.jspecify.annotations.Nullable;

/** Typed mob-effect instance used by potion and consume-effect components. */
public final class ItemEffectInstance implements ItemComponentData {

    private final Key effect;
    private final int duration;
    private final int amplifier;
    private final boolean ambient;
    private final boolean showParticles;
    private final boolean showIcon;
    private final @Nullable ItemEffectInstance hiddenEffect;

    public ItemEffectInstance(
            Key effect,
            int duration,
            int amplifier,
            boolean ambient,
            boolean showParticles,
            boolean showIcon,
            @Nullable ItemEffectInstance hiddenEffect) {
        this.effect = Objects.requireNonNull(effect, "effect");
        this.duration = duration;
        this.amplifier = amplifier;
        this.ambient = ambient;
        this.showParticles = showParticles;
        this.showIcon = showIcon;
        this.hiddenEffect = hiddenEffect;
        if (amplifier < 0 || amplifier > 255) {
            throw new IllegalArgumentException("amplifier must be in 0..255");
        }
    }

    public ItemEffectInstance(Key effect, int duration) {
        this(effect, duration, 0, false, true, true, null);
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
            @Nullable ItemEffectInstance hiddenEffect) {
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
                object.has("hidden_effect") ? ItemEffectInstance.fromJson(object.get("hidden_effect")) : null);
    }

    public Key effect() {
        return effect;
    }

    public int duration() {
        return duration;
    }

    public int amplifier() {
        return amplifier;
    }

    public boolean ambient() {
        return ambient;
    }

    public boolean showParticles() {
        return showParticles;
    }

    public boolean showIcon() {
        return showIcon;
    }

    public Optional<ItemEffectInstance> hiddenEffect() {
        return Optional.ofNullable(hiddenEffect);
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
        if (hiddenEffect != null) {
            json.add("hidden_effect", hiddenEffect.toJson());
        }
        return json;
    }

    @Override
    public boolean equals(@Nullable Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof ItemEffectInstance that)) {
            return false;
        }
        return duration == that.duration
                && amplifier == that.amplifier
                && ambient == that.ambient
                && showParticles == that.showParticles
                && showIcon == that.showIcon
                && effect.equals(that.effect)
                && Objects.equals(hiddenEffect, that.hiddenEffect);
    }

    @Override
    public int hashCode() {
        return Objects.hash(effect, duration, amplifier, ambient, showParticles, showIcon, hiddenEffect);
    }

    @Override
    public String toString() {
        return "ItemEffectInstance[effect=" + effect
                + ", duration=" + duration
                + ", amplifier=" + amplifier
                + ", ambient=" + ambient
                + ", showParticles=" + showParticles
                + ", showIcon=" + showIcon
                + ", hiddenEffect=" + hiddenEffect()
                + "]";
    }
}
