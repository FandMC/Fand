package io.fand.server.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class FandConfigTest {

    @TempDir
    Path tempDir;

    @Test
    void writesDefaultsWhenFileIsMissing() throws Exception {
        var path = tempDir.resolve("fand.yml");

        var config = FandConfig.load(path);

        assertThat(config.identity.brand).isEqualTo("Fand");
        assertThat(config.plugins.directory).isEqualTo("plugins");
        assertThat(config.scheduler.asyncThreads).isZero();
        assertThat(Files.readString(path))
                .contains("# Public-facing identity settings.")
                .contains("identity:")
                .contains("brand: Fand")
                .contains("plugins:")
                .contains("directory: plugins")
                .contains("scheduler:")
                .contains("asyncThreads: 0");
    }

    @Test
    void loadsConfiguredValues() throws Exception {
        var path = tempDir.resolve("fand.yml");
        Files.writeString(path, """
                identity:
                  brand: 'My Fand'

                plugins:
                  directory: 'mods/plugins'

                scheduler:
                  asyncThreads: 6
                """);

        var config = FandConfig.load(path);

        assertThat(config.identity.brand).isEqualTo("My Fand");
        assertThat(config.plugins.directory).isEqualTo("mods/plugins");
        assertThat(config.scheduler.asyncThreads).isEqualTo(6);
    }

    @Test
    void rejectsOutOfRangeValues() throws Exception {
        var path = tempDir.resolve("fand.yml");
        Files.writeString(path, """
                scheduler:
                  asyncThreads: -1
                """);

        assertThatThrownBy(() -> FandConfig.load(path))
                .isInstanceOf(ConfigException.class)
                .hasMessageContaining("scheduler.asyncThreads");
    }
}
