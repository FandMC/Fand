package io.fand.api.item.component;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.Objects;
import java.util.Optional;
import net.kyori.adventure.key.Key;
import org.jspecify.annotations.Nullable;

/** Typed value for {@code minecraft:use_cooldown}. */
public final class ItemUseCooldown implements ItemComponentData {

    private final float seconds;
    private final @Nullable Key cooldownGroup;

    public ItemUseCooldown(float seconds, @Nullable Key cooldownGroup) {
        if (seconds <= 0.0F) {
            throw new IllegalArgumentException("seconds must be > 0");
        }
        this.seconds = seconds;
        this.cooldownGroup = cooldownGroup;
    }

    public ItemUseCooldown(float seconds) {
        this(seconds, null);
    }

    public static ItemUseCooldown fromJson(JsonElement value) {
        if (value == null || !value.isJsonObject()) {
            throw new IllegalArgumentException("use cooldown must be a JSON object");
        }
        var object = value.getAsJsonObject();
        return new ItemUseCooldown(
                object.get("seconds").getAsFloat(),
                object.has("cooldown_group") ? Key.key(object.get("cooldown_group").getAsString()) : null);
    }

    public float seconds() {
        return seconds;
    }

    public Optional<Key> cooldownGroup() {
        return Optional.ofNullable(cooldownGroup);
    }

    @Override
    public JsonObject toJson() {
        var json = new JsonObject();
        json.addProperty("seconds", seconds);
        if (cooldownGroup != null) {
            json.addProperty("cooldown_group", cooldownGroup.asString());
        }
        return json;
    }

    @Override
    public boolean equals(@Nullable Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof ItemUseCooldown that)) {
            return false;
        }
        return Float.compare(seconds, that.seconds) == 0 && Objects.equals(cooldownGroup, that.cooldownGroup);
    }

    @Override
    public int hashCode() {
        return Objects.hash(seconds, cooldownGroup);
    }

    @Override
    public String toString() {
        return "ItemUseCooldown[seconds=" + seconds + ", cooldownGroup=" + cooldownGroup() + "]";
    }
}
