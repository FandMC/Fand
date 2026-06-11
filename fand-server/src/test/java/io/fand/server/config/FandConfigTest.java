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
        assertThat(config.plugins.continueOnLoadFailure).isTrue();
        assertThat(config.plugins.continueOnEnableFailure).isTrue();
        assertThat(config.plugins.logSummary).isTrue();
        assertThat(config.scheduler.asyncThreads).isZero();
        assertThat(config.chunks.workerThreads).isZero();
        assertThat(config.chunks.trackingDiffApplyBudget).isEqualTo(256);
        assertThat(config.chunks.worldgenParallelism).isZero();
        assertThat(config.console.gui.enabled).isTrue();
        assertThat(config.console.gui.theme).isEqualTo("system");
        assertThat(config.network.forwarding.mode).isEqualTo("none");
        assertThat(config.network.forwarding.secret).isEmpty();
        assertThat(config.performance.entityHardCollisionCandidateIndex).isTrue();
        assertThat(config.performance.entitySectionChunkScan).isTrue();
        assertThat(config.performance.entityCollisionAbortPropagation).isTrue();
        assertThat(config.performance.pushableEntityConsumer).isTrue();
        assertThat(config.performance.entityMovementLazyColliders).isTrue();
        assertThat(config.performance.chunkGenerationTaskPlanCache).isTrue();
        assertThat(config.performance.chunkTaskDispatcherBatchLoop).isTrue();
        assertThat(config.performance.chunkStorageRegionScanFastPath).isTrue();
        assertThat(Files.readString(path))
                .contains("# Public-facing identity settings.")
                .contains("identity:")
                .contains("brand: Fand")
                .contains("plugins:")
                .contains("directory: plugins")
                .contains("continueOnLoadFailure: true")
                .contains("continueOnEnableFailure: true")
                .contains("logSummary: true")
                .contains("scheduler:")
                .contains("asyncThreads: 0")
                .contains("chunks:")
                .contains("workerThreads: 0")
                .contains("trackingDiffApplyBudget: 256")
                .contains("worldgenParallelism: 0")
                .contains("console:")
                .contains("gui:")
                .contains("enabled: true")
                .contains("theme: system")
                .contains("network:")
                .contains("forwarding:")
                .contains("mode: none")
                .contains("secret: ''")
                .contains("performance:")
                .contains("entityHardCollisionCandidateIndex: true")
                .contains("entitySectionChunkScan: true")
                .contains("entityCollisionAbortPropagation: true")
                .contains("pushableEntityConsumer: true")
                .contains("entityMovementLazyColliders: true")
                .contains("chunkGenerationTaskPlanCache: true")
                .contains("chunkTaskDispatcherBatchLoop: true")
                .contains("chunkStorageRegionScanFastPath: true");
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

                chunks:
                  workerThreads: 3
                  trackingDiffApplyBudget: 64
                  worldgenParallelism: 6

                console:
                  gui:
                    enabled: false
                    theme: dark

                network:
                  forwarding:
                    mode: velocity-modern
                    secret: 'shared-secret'

                performance:
                  entityHardCollisionCandidateIndex: false
                  entitySectionChunkScan: false
                  entityCollisionAbortPropagation: false
                  pushableEntityConsumer: false
                  entityMovementLazyColliders: false
                  chunkGenerationTaskPlanCache: false
                  chunkTaskDispatcherBatchLoop: false
                  chunkStorageRegionScanFastPath: false
                """);

        var config = FandConfig.load(path);

        assertThat(config.identity.brand).isEqualTo("My Fand");
        assertThat(config.plugins.directory).isEqualTo("mods/plugins");
        assertThat(config.plugins.continueOnLoadFailure).isTrue();
        assertThat(config.plugins.continueOnEnableFailure).isTrue();
        assertThat(config.plugins.logSummary).isFalse();
        assertThat(config.scheduler.asyncThreads).isEqualTo(6);
        assertThat(config.chunks.workerThreads).isEqualTo(3);
        assertThat(config.chunks.trackingDiffApplyBudget).isEqualTo(64);
        assertThat(config.chunks.worldgenParallelism).isEqualTo(6);
        assertThat(config.console.gui.enabled).isFalse();
        assertThat(config.console.gui.theme).isEqualTo("dark");
        assertThat(config.network.forwarding.mode).isEqualTo("velocity-modern");
        assertThat(config.network.forwarding.secret).isEqualTo("shared-secret");
        assertThat(config.performance.entityHardCollisionCandidateIndex).isFalse();
        assertThat(config.performance.entitySectionChunkScan).isFalse();
        assertThat(config.performance.entityCollisionAbortPropagation).isFalse();
        assertThat(config.performance.pushableEntityConsumer).isFalse();
        assertThat(config.performance.entityMovementLazyColliders).isFalse();
        assertThat(config.performance.chunkGenerationTaskPlanCache).isFalse();
        assertThat(config.performance.chunkTaskDispatcherBatchLoop).isFalse();
        assertThat(config.performance.chunkStorageRegionScanFastPath).isFalse();
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
