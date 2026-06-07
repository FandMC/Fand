package io.fand.api.item.component;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import net.kyori.adventure.key.Key;
import org.junit.jupiter.api.Test;

class ItemComponentsTest {

    @Test
    void roundTripsPatchJson() {
        var json = new JsonObject();
        json.add("minecraft:custom_name", new JsonPrimitive("name"));
        json.add("!minecraft:lore", new JsonObject());

        var components = ItemComponents.fromJsonPatch(json);
        var encoded = components.toJsonPatch();

        assertThat(components.has(ItemComponentKeys.CUSTOM_NAME)).isTrue();
        assertThat(components.removes(ItemComponentKeys.LORE)).isTrue();
        assertThat(encoded.get("minecraft:custom_name").getAsString()).isEqualTo("name");
        assertThat(encoded.has("!minecraft:lore")).isTrue();
    }

    @Test
    void defensiveCopiesValues() {
        var value = new JsonObject();
        value.addProperty("before", true);

        var components = ItemComponents.of(Key.key("minecraft:custom_data"), value);
        value.addProperty("after", true);

        assertThat(components.get(Key.key("minecraft:custom_data")).orElseThrow().getAsJsonObject().has("after"))
                .isFalse();
    }
}
