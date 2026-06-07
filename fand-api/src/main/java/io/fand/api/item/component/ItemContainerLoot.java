package io.fand.api.item.component;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.Objects;
import net.kyori.adventure.key.Key;

/** Typed value for {@code minecraft:container_loot}. */
public record ItemContainerLoot(Key lootTable, long seed) implements ItemComponentData {

    public ItemContainerLoot {
        lootTable = Objects.requireNonNull(lootTable, "lootTable");
    }

    public static ItemContainerLoot fromJson(JsonElement value) {
        if (value == null || !value.isJsonObject()) {
            throw new IllegalArgumentException("container loot must be a JSON object");
        }
        var object = value.getAsJsonObject();
        return new ItemContainerLoot(
                Key.key(object.get("loot_table").getAsString()),
                object.has("seed") ? object.get("seed").getAsLong() : 0L);
    }

    @Override
    public JsonObject toJson() {
        var json = new JsonObject();
        json.addProperty("loot_table", lootTable.asString());
        if (seed != 0L) {
            json.addProperty("seed", seed);
        }
        return json;
    }
}
