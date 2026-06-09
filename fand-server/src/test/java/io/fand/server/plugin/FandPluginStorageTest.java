package io.fand.server.plugin;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.gson.JsonPrimitive;
import java.util.UUID;
import net.kyori.adventure.key.Key;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FandPluginStorageTest {

    @TempDir
    java.nio.file.Path tempDir;

    @Test
    void storesJsonValuesByScope() {
        var storage = new FandPluginStorage(tempDir);
        storage.global().setString("version", "1");
        storage.player(UUID.fromString("00000000-0000-0000-0000-000000000123")).setInt("research", 7);
        storage.chunk(Key.key("minecraft:overworld"), 1, -2).set("energy", new JsonPrimitive(42));

        var reloaded = new FandPluginStorage(tempDir);

        assertThat(reloaded.global().getString("version")).contains("1");
        assertThat(reloaded.player(UUID.fromString("00000000-0000-0000-0000-000000000123")).getInt("research")).contains(7);
        assertThat(reloaded.chunk(Key.key("minecraft:overworld"), 1, -2).getInt("energy")).contains(42);
    }
}
