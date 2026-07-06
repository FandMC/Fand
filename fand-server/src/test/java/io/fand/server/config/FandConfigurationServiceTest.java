package io.fand.server.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.fand.api.config.ConfigurationException;
import io.fand.api.config.ConfigurationFormat;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class FandConfigurationServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void detectsSupportedExtensions() {
        assertThat(ConfigurationFormat.findByExtension("config.yml")).contains(ConfigurationFormat.YAML);
        assertThat(ConfigurationFormat.findByExtension("config.yaml")).contains(ConfigurationFormat.YAML);
        assertThat(ConfigurationFormat.findByExtension("settings.json")).contains(ConfigurationFormat.JSON);
        assertThat(ConfigurationFormat.findByExtension("rules.toml")).contains(ConfigurationFormat.TOML);
        assertThat(ConfigurationFormat.findByExtension("messages.properties")).contains(ConfigurationFormat.PROPERTIES);
    }

    @Test
    void unknownExtensionThrows() {
        assertThatThrownBy(() -> FandConfigurationService.INSTANCE.load(tempDir.resolve("config.ini")))
                .isInstanceOf(ConfigurationException.class)
                .hasMessageContaining("Unsupported configuration file extension");
    }

    @Test
    void jsonRoundTripThroughUnifiedService() throws Exception {
        var path = tempDir.resolve("settings.json");
        Files.writeString(path, """
                {
                  "server": {
                    "host": "example.com",
                    "port": 25565,
                    "flags": ["online", "whitelist"]
                  },
                  "rate": 1.5,
                  "enabled": true
                }
                """);

        var config = FandConfigurationService.INSTANCE.load(path);

        assertThat(config).isInstanceOf(JsonConfiguration.class);
        assertStandardValues(config);

        config.set("server.motd", "hello");
        config.save();

        var reloaded = FandConfigurationService.INSTANCE.load(path);
        assertThat(reloaded.string("server.motd", "")).isEqualTo("hello");
        assertStandardValues(reloaded);
    }

    @Test
    void yamlRoundTripThroughUnifiedService() throws Exception {
        var path = tempDir.resolve("settings.yml");
        Files.writeString(path, """
                server:
                  host: example.com
                  port: 25565
                  flags:
                    - online
                    - whitelist
                rate: 1.5
                enabled: true
                """);

        var config = FandConfigurationService.INSTANCE.load(path);

        assertThat(config).isInstanceOf(YamlConfiguration.class);
        assertStandardValues(config);

        config.set("server.motd", "hello");
        config.save();

        var reloaded = FandConfigurationService.INSTANCE.load(path);
        assertThat(reloaded.string("server.motd", "")).isEqualTo("hello");
        assertStandardValues(reloaded);
    }

    @Test
    void tomlRoundTripThroughUnifiedService() throws Exception {
        var path = tempDir.resolve("settings.toml");
        Files.writeString(path, """
                rate = 1.5
                enabled = true

                [server]
                host = "example.com"
                port = 25565
                flags = ["online", "whitelist"]
                """);

        var config = FandConfigurationService.INSTANCE.load(path);

        assertThat(config).isInstanceOf(TomlConfiguration.class);
        assertStandardValues(config);

        config.set("server.motd", "hello");
        config.save();

        var reloaded = FandConfigurationService.INSTANCE.load(path);
        assertThat(reloaded.string("server.motd", "")).isEqualTo("hello");
        assertStandardValues(reloaded);
    }

    @Test
    void propertiesRoundTripThroughUnifiedService() throws Exception {
        var path = tempDir.resolve("settings.properties");
        Files.writeString(path, """
                enabled=true
                rate=1.5
                server.flags.0=online
                server.flags.1=whitelist
                server.host=example.com
                server.port=25565
                """);

        var config = FandConfigurationService.INSTANCE.load(path);

        assertThat(config).isInstanceOf(PropertiesConfiguration.class);
        assertStandardValues(config);

        config.set("server.motd", "hello");
        config.set("extra.flags", List.of("a", "b"));
        config.save();

        var reloaded = FandConfigurationService.INSTANCE.load(path);
        assertThat(reloaded.string("server.motd", "")).isEqualTo("hello");
        assertThat(reloaded.stringList("extra.flags")).containsExactly("a", "b");
        assertStandardValues(reloaded);
    }

    @Test
    void loadOrCopyDefaultUsesDetectedFormat() throws Exception {
        var path = tempDir.resolve("messages.properties");
        var defaults = "welcome=hello\n".getBytes(StandardCharsets.UTF_8);

        var config = FandConfigurationService.INSTANCE.loadOrCopyDefault(
                path,
                new java.io.ByteArrayInputStream(defaults));

        assertThat(Files.readString(path)).isEqualTo("welcome=hello\n");
        assertThat(config.string("welcome", "")).isEqualTo("hello");
    }

    private static void assertStandardValues(io.fand.api.config.Configuration config) {
        assertThat(config.string("server.host", "")).isEqualTo("example.com");
        assertThat(config.intValue("server.port", 0)).isEqualTo(25565);
        assertThat(config.doubleValue("rate", 0.0)).isEqualTo(1.5);
        assertThat(config.booleanValue("enabled", false)).isTrue();
        assertThat(config.stringList("server.flags")).containsExactly("online", "whitelist");
    }
}
