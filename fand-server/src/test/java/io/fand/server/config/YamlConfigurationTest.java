package io.fand.server.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.fand.api.config.ConfigurationException;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class YamlConfigurationTest {

    @TempDir
    Path tempDir;

    @Test
    void emptyWhenFileMissing() {
        var config = YamlConfiguration.load(tempDir.resolve("missing.yml"));

        assertThat(config.keys()).isEmpty();
        assertThat(config.string("anything", "fallback")).isEqualTo("fallback");
    }

    @Test
    void readsScalarsAndDotPaths() throws Exception {
        var path = tempDir.resolve("config.yml");
        Files.writeString(path, """
                server:
                  host: example.com
                  port: 25565
                  flags:
                    - online
                    - whitelist
                  numbers:
                    - 1
                    - "2"
                    - nope
                rate: 1.5
                enabled: true
                """);

        var config = YamlConfiguration.load(path);

        assertThat(config.string("server.host", "")).isEqualTo("example.com");
        assertThat(config.intValue("server.port", 0)).isEqualTo(25565);
        assertThat(config.doubleValue("rate", 0.0)).isEqualTo(1.5);
        assertThat(config.booleanValue("enabled", false)).isTrue();
        assertThat(config.stringList("server.flags")).containsExactly("online", "whitelist");
        assertThat(config.stringList("server.missing-flags", List.of("fallback"))).containsExactly("fallback");
        assertThat(config.intList("server.numbers")).containsExactly(1, 2);
        assertThat(config.intList("server.missing-numbers", List.of(7, 8))).containsExactly(7, 8);
        assertThat(config.contains("server.host")).isTrue();
        assertThat(config.contains("server.unknown")).isFalse();
    }

    @Test
    void typeMismatchReturnsDefault() throws Exception {
        var path = tempDir.resolve("config.yml");
        Files.writeString(path, "port: not-a-number\n");

        var config = YamlConfiguration.load(path);

        assertThat(config.intValue("port", 42)).isEqualTo(42);
        assertThat(config.stringList("port")).isEmpty();
    }

    @Test
    void setAndSaveRoundTrip() throws Exception {
        var path = tempDir.resolve("config.yml");
        var config = YamlConfiguration.load(path);

        config.set("server.host", "example.com");
        config.set("server.port", 25565);
        config.set("flags", List.of("a", "b"));
        config.save();

        var reloaded = YamlConfiguration.load(path);
        assertThat(reloaded.string("server.host", "")).isEqualTo("example.com");
        assertThat(reloaded.intValue("server.port", 0)).isEqualTo(25565);
        assertThat(reloaded.stringList("flags")).containsExactly("a", "b");
    }

    @Test
    void getSectionCreatesMissing() {
        var config = YamlConfiguration.load(tempDir.resolve("config.yml"));
        var section = config.section("nested.deep");

        section.set("value", "x");
        assertThat(config.string("nested.deep.value", "")).isEqualTo("x");
    }

    @Test
    void copyDefaultMaterialisesFromInputStream() throws Exception {
        var path = tempDir.resolve("config.yml");
        var defaults = "greeting: hello\n".getBytes(StandardCharsets.UTF_8);

        var config = YamlConfiguration.loadOrCopyDefault(path, new ByteArrayInputStream(defaults));

        assertThat(Files.readString(path)).isEqualTo("greeting: hello\n");
        assertThat(config.string("greeting", "")).isEqualTo("hello");
    }

    @Test
    void invalidYamlThrows() throws Exception {
        var path = tempDir.resolve("config.yml");
        Files.writeString(path, "[just a list]\n");

        assertThatThrownBy(() -> YamlConfiguration.load(path))
                .isInstanceOf(ConfigurationException.class)
                .hasMessageContaining("must be a YAML mapping");
    }
}
