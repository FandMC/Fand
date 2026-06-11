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
    void reloadsHotApplicableValuesAndFlagsOnlyPluginDirectoryRestartRequired() throws Exception {
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

                chunks:
                  workerThreads: 0
                  trackingDiffApplyBudget: 256
                  worldgenParallelism: 0
                  dedicatedLightThread: true
                  lightTaskQueueFastPath: true

                network:
                  forwarding:
                    mode: none
                    secret: ''
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

                chunks:
                  workerThreads: 2
                  trackingDiffApplyBudget: 32
                  worldgenParallelism: 4
                  dedicatedLightThread: false
                  lightTaskQueueFastPath: false

                network:
                  forwarding:
                    mode: velocity-modern
                    secret: 'shared-secret'
                """);

        var result = server.reloadConfig();

        assertThat(server.brand()).isEqualTo("Reloaded Fand");
        assertThat(result.hotApplied()).containsExactlyInAnyOrder(
                "identity.brand",
                "plugins.continueOnLoadFailure",
                "plugins.continueOnEnableFailure",
                "plugins.logSummary",
                "scheduler.asyncThreads",
                "chunks.workerThreads",
                "chunks.trackingDiffApplyBudget"
        );
        assertThat(result.requiresRestart()).containsExactlyInAnyOrder(
                "plugins.directory",
                "chunks.worldgenParallelism",
                "chunks.dedicatedLightThread",
                "chunks.lightTaskQueueFastPath",
                "network.forwarding.mode",
                "network.forwarding.secret"
        );
        assertThat(result.restartRequired()).isTrue();
        assertThat(result.changed()).isTrue();
    }

    @Test
    void reloadsEntityPerformanceOptionsAsHotApplicable() throws Exception {
        var path = tempDir.resolve("fand.yml");
        Files.writeString(path, """
                performance:
                  entityHardCollisionCandidateIndex: false
                  entitySectionChunkScan: false
                  entityCollisionAbortPropagation: false
                  pushableEntityConsumer: false
                  entityMovementLazyColliders: false
                  entityTrackerFastPath: false
                  deepPassengerIteration: false
                  entityTypeLookupFastPath: false
                  randomTickPositionMask: false
                  chunkGenerationTaskPlanCache: false
                  chunkTaskDispatcherBatchLoop: false
                  chunkStorageRegionScanFastPath: false
                """);

        var initial = FandConfig.load(path);
        var server = new FandServer(path, initial, getClass().getClassLoader());

        Files.writeString(path, """
                performance:
                  entityHardCollisionCandidateIndex: true
                  entitySectionChunkScan: true
                  entityCollisionAbortPropagation: true
                  pushableEntityConsumer: true
                  entityMovementLazyColliders: true
                  entityTrackerFastPath: true
                  deepPassengerIteration: true
                  entityTypeLookupFastPath: true
                  randomTickPositionMask: true
                  chunkGenerationTaskPlanCache: true
                  chunkTaskDispatcherBatchLoop: true
                  chunkStorageRegionScanFastPath: true
                """);

        var result = server.reloadConfig();

        assertThat(result.hotApplied()).containsExactlyInAnyOrder(
                "performance.entityHardCollisionCandidateIndex",
                "performance.entitySectionChunkScan",
                "performance.entityCollisionAbortPropagation",
                "performance.pushableEntityConsumer",
                "performance.entityMovementLazyColliders",
                "performance.entityTrackerFastPath",
                "performance.deepPassengerIteration",
                "performance.entityTypeLookupFastPath",
                "performance.randomTickPositionMask",
                "performance.chunkGenerationTaskPlanCache",
                "performance.chunkTaskDispatcherBatchLoop",
                "performance.chunkStorageRegionScanFastPath"
        );
        assertThat(result.requiresRestart()).isEmpty();
    }
}
