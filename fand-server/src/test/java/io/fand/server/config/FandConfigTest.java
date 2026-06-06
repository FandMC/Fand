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
        assertThat(config.plugins.continueOnLoadFailure).isFalse();
        assertThat(config.plugins.continueOnEnableFailure).isFalse();
        assertThat(config.plugins.logSummary).isTrue();
        assertThat(config.scheduler.asyncThreads).isZero();
        assertThat(config.network.forwarding.mode).isEqualTo("none");
        assertThat(config.network.forwarding.secret).isEmpty();
        assertThat(Files.readString(path))
                .contains("# Public-facing identity settings.")
                .contains("identity:")
                .contains("brand: Fand")
                .contains("plugins:")
                .contains("directory: plugins")
                .contains("continueOnLoadFailure: false")
                .contains("continueOnEnableFailure: false")
                .contains("logSummary: true")
                .contains("scheduler:")
                .contains("asyncThreads: 0")
                .contains("network:")
                .contains("forwarding:")
                .contains("mode: none")
                .contains("secret: ''");
    }

    @Test
    void loadsConfiguredValues() throws Exception {
        var path = tempDir.resolve("fand.yml");
        Files.writeString(path, """
                identity:
                  brand: 'My Fand'

                plugins:
                  directory: 'mods/plugins'
                  continueOnLoadFailure: true
                  continueOnEnableFailure: true
                  logSummary: false

                scheduler:
                  asyncThreads: 6

                network:
                  forwarding:
                    mode: velocity-modern
                    secret: 'shared-secret'
                """);

        var config = FandConfig.load(path);

        assertThat(config.identity.brand).isEqualTo("My Fand");
        assertThat(config.plugins.directory).isEqualTo("mods/plugins");
        assertThat(config.plugins.continueOnLoadFailure).isTrue();
        assertThat(config.plugins.continueOnEnableFailure).isTrue();
        assertThat(config.plugins.logSummary).isFalse();
        assertThat(config.scheduler.asyncThreads).isEqualTo(6);
        assertThat(config.network.forwarding.mode).isEqualTo("velocity-modern");
        assertThat(config.network.forwarding.secret).isEqualTo("shared-secret");
    }

    @Test
    void acceptsBungeeForwardingAlias() throws Exception {
        var path = tempDir.resolve("fand.yml");
        Files.writeString(path, """
                network:
                  forwarding:
                    mode: bc
                    secret: ''
                """);

        var config = FandConfig.load(path);

        assertThat(config.network.forwarding.mode).isEqualTo("bc");
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

    @Test
    void rejectsVelocityForwardingWithoutSecret() throws Exception {
        var path = tempDir.resolve("fand.yml");
        Files.writeString(path, """
                network:
                  forwarding:
                    mode: velocity-modern
                """);

        assertThatThrownBy(() -> FandConfig.load(path))
                .isInstanceOf(ConfigException.class)
                .hasMessageContaining("network.forwarding.secret");
    }
}
