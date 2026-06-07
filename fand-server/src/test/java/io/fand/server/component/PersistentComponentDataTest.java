package io.fand.server.component;

import static org.assertj.core.api.Assertions.assertThat;

import io.fand.api.component.DataComponentKey;
import io.fand.api.component.DataComponentMap;
import net.kyori.adventure.key.Key;
import org.junit.jupiter.api.Test;

class PersistentComponentDataTest {

    private static final DataComponentKey<String> LABEL =
            DataComponentKey.string(Key.key("fand-test:label"));
    private static final DataComponentKey<Integer> COUNT =
            DataComponentKey.integer(Key.key("fand-test:count"));

    @Test
    void storesAndLoadsComponentMapsById() {
        var data = new PersistentComponentData();
        data.put("demo", DataComponentMap.empty()
                .with(LABEL, "machine")
                .with(COUNT, 7));

        var loaded = data.get("demo");

        assertThat(loaded.get(LABEL)).contains("machine");
        assertThat(loaded.get(COUNT)).contains(7);
        assertThat(data.isDirty()).isTrue();
    }

    @Test
    void emptyMapsRemoveStoredEntries() {
        var data = new PersistentComponentData();
        data.put("demo", DataComponentMap.of(LABEL, "machine"));

        data.put("demo", DataComponentMap.empty());

        assertThat(data.get("demo").isEmpty()).isTrue();
    }

    @Test
    void savedDataCodecRoundTrips() {
        var data = new PersistentComponentData();
        data.put("demo", DataComponentMap.of(LABEL, "machine"));

        var encoded = PersistentComponentData.CODEC.encodeStart(com.mojang.serialization.JsonOps.INSTANCE, data)
                .getOrThrow();
        var decoded = PersistentComponentData.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE, encoded)
                .getOrThrow();

        assertThat(decoded.get("demo").get(LABEL)).contains("machine");
    }
}
