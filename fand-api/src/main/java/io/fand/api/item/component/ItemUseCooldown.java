package io.fand.api.item.component;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.Objects;
import java.util.Optional;
import net.kyori.adventure.key.Key;

/** Typed value for {@code minecraft:use_cooldown}. */
public record ItemUseCooldown(float seconds, Optional<Key> cooldownGroup) implements ItemComponentData {

    public ItemUseCooldown {
        if (seconds <= 0.0F) {
            throw new IllegalArgumentException("seconds must be > 0");
        }
        cooldownGroup = Objects.requireNonNull(cooldownGroup, "cooldownGroup");
    }

    public ItemUseCooldown(float seconds) {
        this(seconds, Optional.empty());
    }

    public static ItemUseCooldown fromJson(JsonElement value) {
        if (value == null || !value.isJsonObject()) {
            throw new IllegalArgumentException("use cooldown must be a JSON object");
        }
        var object = value.getAsJsonObject();
        return new ItemUseCooldown(
                object.get("seconds").getAsFloat(),
                object.has("cooldown_group")
                        ? Optional.of(Key.key(object.get("cooldown_group").getAsString()))
                        : Optional.empty());
    }

    @Override
    public JsonObject toJson() {
        var json = new JsonObject();
        json.addProperty("seconds", seconds);
        cooldownGroup.ifPresent(group -> json.addProperty("cooldown_group", group.asString()));
        return json;
    }
}
