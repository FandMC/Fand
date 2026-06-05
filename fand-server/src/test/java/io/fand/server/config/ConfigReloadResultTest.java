package io.fand.server;

import static org.assertj.core.api.Assertions.assertThat;

import io.fand.server.config.FandConfig;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class ConfigReloadResultTest {

    @TempDir
    Path tempDir;

    @Test
    void reloadsHotApplicableValuesAndFlagsRestartRequiredOnOthers() throws Exception {
        var path = tempDir.resolve("fand.yml");
        Files.writeString(path, """
                identity:
                  brand: Fand

                plugins:
                  directory: plugins
                  continueOnLoadFailure: false
                  continueOnEnableFailure: false
                  logSummary: true

                scheduler:
                  asyncThreads: 0
                """);

        var initial = FandConfig.load(path);
        var server = new FandServer(path, initial, getClass().getClassLoader());

        Files.writeString(path, """
                identity:
                  brand: 'Reloaded Fand'

                plugins:
                  directory: alt-plugins
                  continueOnLoadFailure: true
                  continueOnEnableFailure: true
                  logSummary: false

                scheduler:
                  asyncThreads: 8
                """);

        var result = server.reloadConfig();

        assertThat(server.brand()).isEqualTo("Reloaded Fand");
        assertThat(result.hotApplied()).containsExactlyInAnyOrder(
                "identity.brand",
                "plugins.continueOnLoadFailure",
                "plugins.continueOnEnableFailure",
                "plugins.logSummary"
        );
        assertThat(result.requiresRestart()).containsExactlyInAnyOrder(
                "plugins.directory",
                "scheduler.asyncThreads"
        );
        assertThat(result.restartRequired()).isTrue();
        assertThat(result.changed()).isTrue();
    }
}
