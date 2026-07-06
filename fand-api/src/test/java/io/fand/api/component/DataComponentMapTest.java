package io.fand.api.component;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import java.util.UUID;
import net.kyori.adventure.key.Key;
import org.junit.jupiter.api.Test;

class DataComponentMapTest {

    private static final DataComponentKey<String> LABEL =
            DataComponentKey.string(Key.key("fand-test:label"));
    private static final DataComponentKey<Integer> COUNT =
            DataComponentKey.integer(Key.key("fand-test:count"));
    private static final DataComponentKey<JsonObject> DATA =
            DataComponentKey.object(Key.key("fand-test:data"));
    private static final DataComponentKey<UUID> OWNER =
            DataComponentKey.uuid(Key.key("fand-test:owner"));

    @Test
    void roundTripsTypedValues() {
        var map = DataComponentMap.emptyMap()
                .with(LABEL, "machine")
                .with(COUNT, 4);

        assertThat(map.value(LABEL)).contains("machine");
        assertThat(map.value(COUNT)).contains(4);
        assertThat(DataComponentMap.fromJson(map.toJson()).value(LABEL)).contains("machine");
    }

    @Test
    void supportsUuidValues() {
        var id = UUID.fromString("00000000-0000-0000-0000-000000000123");
        var map = DataComponentMap.of(OWNER, id);

        assertThat(map.value(OWNER)).contains(id);
        assertThat(DataComponentMap.fromJson(map.toJson()).value(OWNER)).contains(id);
    }

    @Test
    void defensivelyCopiesJsonValues() {
        var object = new JsonObject();
        object.addProperty("before", true);

        var map = DataComponentMap.emptyMap().with(DATA, object);
        object.addProperty("after", true);

        assertThat(map.value(DATA).orElseThrow().has("after")).isFalse();

        var read = map.value(DATA).orElseThrow();
        read.addProperty("mutated", true);

        assertThat(map.value(DATA).orElseThrow().has("mutated")).isFalse();
    }

    @Test
    void supportsRawJsonAccessForUnknownKeys() {
        var key = Key.key("fand-test:unknown");
        var map = DataComponentMap.of(key, new JsonPrimitive("value"));

        assertThat(map.contains(key)).isTrue();
        assertThat(map.value(key)).contains(new JsonPrimitive("value"));
        assertThat(map.without(key).empty()).isTrue();
    }
}
