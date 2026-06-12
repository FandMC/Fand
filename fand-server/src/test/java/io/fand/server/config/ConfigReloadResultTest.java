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
                  worldgenSeaLevelCache: false
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
                  worldgenSeaLevelCache: true
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
                "performance.chunkStorageRegionScanFastPath",
                "performance.worldgenSeaLevelCache"
        );
        assertThat(result.requiresRestart()).isEmpty();
    }

    @Test
    void reloadsTechnicalOptionsAsHotApplicable() throws Exception {
        var path = tempDir.resolve("fand.yml");
        Files.writeString(path, """
                technical:
                  zeroTickPlants: false
                  oldHopperSuckInBehavior: false
                  shearsInDispenserCanZeroAmount: false
                  allowEntityPortalWithPassenger: true
                  disableGatewayPortalEntityTicking: false
                  disableLivingEntityAiStepAliveCheck: false
                  spawnInvulnerableTime: false
                  oldZombiePiglinDrop: false
                  oldZombieReinforcement: false
                  allowAnvilDestroyItemEntities: false
                  disableItemDamageCheck: false
                  keepLeashConnectWhenUseFirework: false
                  tntWetExplosionNoItemDamage: false
                  oldProjectileExplosionBehavior: false
                  oldThrowableProjectileTickOrder: false
                  oldMinecartMotionBehavior: false
                  copperBulbOneGameTickDelay: false
                  crafterOneGameTickDelay: false
                  noTntPlaceUpdate: false
                  allowPistonDuplication: false
                  allowTntDuplication: false
                  allowRailDuplication: false
                  allowCarpetDuplication: false
                  allowGravityBlockEndPortalDuplication: false
                  redstoneIgnoreUpwardsUpdate: false
                  movableBuddingAmethyst: false
                  stringTripwireHookDuplicate: false
                  tripwireBehavior: vanilla_21
                """);

        var initial = FandConfig.load(path);
        var server = new FandServer(path, initial, getClass().getClassLoader());

        Files.writeString(path, """
                technical:
                  zeroTickPlants: true
                  oldHopperSuckInBehavior: true
                  shearsInDispenserCanZeroAmount: true
                  allowEntityPortalWithPassenger: false
                  disableGatewayPortalEntityTicking: true
                  disableLivingEntityAiStepAliveCheck: true
                  spawnInvulnerableTime: true
                  oldZombiePiglinDrop: true
                  oldZombieReinforcement: true
                  allowAnvilDestroyItemEntities: true
                  disableItemDamageCheck: true
                  keepLeashConnectWhenUseFirework: true
                  tntWetExplosionNoItemDamage: true
                  oldProjectileExplosionBehavior: true
                  oldThrowableProjectileTickOrder: true
                  oldMinecartMotionBehavior: true
                  copperBulbOneGameTickDelay: true
                  crafterOneGameTickDelay: true
                  noTntPlaceUpdate: true
                  allowPistonDuplication: true
                  allowTntDuplication: true
                  allowRailDuplication: true
                  allowCarpetDuplication: true
                  allowGravityBlockEndPortalDuplication: true
                  redstoneIgnoreUpwardsUpdate: true
                  movableBuddingAmethyst: true
                  stringTripwireHookDuplicate: true
                  tripwireBehavior: mixed
                """);

        var result = server.reloadConfig();

        assertThat(result.hotApplied()).containsExactlyInAnyOrder(
                "technical.zeroTickPlants",
                "technical.oldHopperSuckInBehavior",
                "technical.shearsInDispenserCanZeroAmount",
                "technical.allowEntityPortalWithPassenger",
                "technical.disableGatewayPortalEntityTicking",
                "technical.disableLivingEntityAiStepAliveCheck",
                "technical.spawnInvulnerableTime",
                "technical.oldZombiePiglinDrop",
                "technical.oldZombieReinforcement",
                "technical.allowAnvilDestroyItemEntities",
                "technical.disableItemDamageCheck",
                "technical.keepLeashConnectWhenUseFirework",
                "technical.tntWetExplosionNoItemDamage",
                "technical.oldProjectileExplosionBehavior",
                "technical.oldThrowableProjectileTickOrder",
                "technical.oldMinecartMotionBehavior",
                "technical.copperBulbOneGameTickDelay",
                "technical.crafterOneGameTickDelay",
                "technical.noTntPlaceUpdate",
                "technical.allowPistonDuplication",
                "technical.allowTntDuplication",
                "technical.allowRailDuplication",
                "technical.allowCarpetDuplication",
                "technical.allowGravityBlockEndPortalDuplication",
                "technical.redstoneIgnoreUpwardsUpdate",
                "technical.movableBuddingAmethyst",
                "technical.stringTripwireHookDuplicate",
                "technical.tripwireBehavior"
        );
        assertThat(result.requiresRestart()).isEmpty();
    }
}
