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
        data.put("demo", DataComponentMap.emptyMap()
                .with(LABEL, "machine")
                .with(COUNT, 7));

        var loaded = data.get("demo");

        assertThat(loaded.value(LABEL)).contains("machine");
        assertThat(loaded.value(COUNT)).contains(7);
        assertThat(data.isDirty()).isTrue();
    }

    @Test
    void emptyMapsRemoveStoredEntries() {
        var data = new PersistentComponentData();
        data.put("demo", DataComponentMap.of(LABEL, "machine"));

        data.put("demo", DataComponentMap.emptyMap());

        assertThat(data.get("demo").empty()).isTrue();
    }

    @Test
    void savedDataCodecRoundTrips() {
        var data = new PersistentComponentData();
        data.put("demo", DataComponentMap.of(LABEL, "machine"));

        var encoded = PersistentComponentData.CODEC.encodeStart(com.mojang.serialization.JsonOps.INSTANCE, data)
                .getOrThrow();
        var decoded = PersistentComponentData.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE, encoded)
                .getOrThrow();

        assertThat(decoded.get("demo").value(LABEL)).contains("machine");
    }

    @Test
    void indexesIdsByComponentKey() {
        var data = new PersistentComponentData();
        data.put("one", DataComponentMap.of(LABEL, "machine"));
        data.put("two", DataComponentMap.of(COUNT, 2));
        data.put("three", DataComponentMap.emptyMap().with(LABEL, "other").with(COUNT, 3));

        assertThat(data.idsWith(LABEL.key())).containsExactly("one", "three");
        assertThat(data.entries().keySet()).containsExactly("one", "three", "two");
    }
}
