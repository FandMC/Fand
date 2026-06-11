package io.fand.server.config;

import io.fand.server.console.gui.GuiTheme;
import io.fand.server.console.gui.GuiThemeService;
import io.fand.server.chunk.ChunkSendScheduler;
import io.fand.server.network.ProxyForwardingMode;
import io.fand.server.plugin.PluginRuntime;
import io.fand.server.scheduler.TaskScheduler;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Diffs an in-memory {@link FandConfig} against the freshly loaded one and
 * tags each changed key as either hot-applied or requires-restart.
 *
 * <p>Centralises the rule "which fields are safe to swap at runtime" so the
 * lifecycle owner ({@code FandServer}) does not grow with each new config key.
 */
public final class ConfigReloader {

    private final Path configPath;
    private final AtomicReference<FandConfig> current;
    private final PluginRuntime plugins;
    private final TaskScheduler scheduler;
    private final ChunkSendScheduler chunks;
    private final GuiThemeService guiThemes;

    public ConfigReloader(
            Path configPath,
            AtomicReference<FandConfig> current,
            PluginRuntime plugins,
            TaskScheduler scheduler,
            ChunkSendScheduler chunks,
            GuiThemeService guiThemes
    ) {
        this.configPath = configPath;
        this.current = current;
        this.plugins = plugins;
        this.scheduler = scheduler;
        this.chunks = chunks;
        this.guiThemes = guiThemes;
    }

    public ConfigReloadResult reload() {
        var previous = current.get();
        var reloaded = FandConfig.load(configPath);
        var hotApplied = new ArrayList<String>();
        var requiresRestart = new ArrayList<String>();

        if (!previous.identity.brand.equals(reloaded.identity.brand)) {
            hotApplied.add("identity.brand");
        }
        if (previous.plugins.continueOnLoadFailure != reloaded.plugins.continueOnLoadFailure) {
            hotApplied.add("plugins.continueOnLoadFailure");
        }
        if (previous.plugins.continueOnEnableFailure != reloaded.plugins.continueOnEnableFailure) {
            hotApplied.add("plugins.continueOnEnableFailure");
        }
        if (previous.plugins.logSummary != reloaded.plugins.logSummary) {
            hotApplied.add("plugins.logSummary");
        }
        if (!previous.plugins.directory.equals(reloaded.plugins.directory)) {
            requiresRestart.add("plugins.directory");
        }
        if (previous.scheduler.asyncThreads != reloaded.scheduler.asyncThreads) {
            scheduler.reconfigureAsyncThreads(reloaded.scheduler.asyncThreads);
            hotApplied.add("scheduler.asyncThreads");
        }
        if (previous.chunks.workerThreads != reloaded.chunks.workerThreads) {
            hotApplied.add("chunks.workerThreads");
        }
        if (previous.chunks.trackingDiffApplyBudget != reloaded.chunks.trackingDiffApplyBudget) {
            hotApplied.add("chunks.trackingDiffApplyBudget");
        }
        if (previous.chunks.worldgenParallelism != reloaded.chunks.worldgenParallelism) {
            requiresRestart.add("chunks.worldgenParallelism");
        }
        if (previous.chunks.dedicatedLightThread != reloaded.chunks.dedicatedLightThread) {
            requiresRestart.add("chunks.dedicatedLightThread");
        }
        if (previous.chunks.lightTaskQueueFastPath != reloaded.chunks.lightTaskQueueFastPath) {
            requiresRestart.add("chunks.lightTaskQueueFastPath");
        }
        if (previous.chunks.workerThreads != reloaded.chunks.workerThreads
                || previous.chunks.trackingDiffApplyBudget != reloaded.chunks.trackingDiffApplyBudget) {
            chunks.reconfigure(reloaded.chunks);
        }
        io.fand.server.hooks.FandHooks.applyChunkConfig(reloaded.chunks);
        if (ProxyForwardingMode.fromConfig(previous.network.forwarding.mode)
                != ProxyForwardingMode.fromConfig(reloaded.network.forwarding.mode)) {
            requiresRestart.add("network.forwarding.mode");
        }
        if (!previous.network.forwarding.secret.equals(reloaded.network.forwarding.secret)) {
            requiresRestart.add("network.forwarding.secret");
        }
        var previousTheme = GuiTheme.fromConfig(previous.console.gui.theme);
        var reloadedTheme = GuiTheme.fromConfig(reloaded.console.gui.theme);
        if (previousTheme != reloadedTheme) {
            guiThemes.set(reloadedTheme);
            hotApplied.add("console.gui.theme");
        }
        if (previous.console.gui.enabled != reloaded.console.gui.enabled) {
            requiresRestart.add("console.gui.enabled");
        }
        if (previous.performance.explosionDensityCache != reloaded.performance.explosionDensityCache) {
            hotApplied.add("performance.explosionDensityCache");
        }
        if (previous.performance.collisionTeamCache != reloaded.performance.collisionTeamCache) {
            hotApplied.add("performance.collisionTeamCache");
        }
        if (previous.performance.explosionBlockCache != reloaded.performance.explosionBlockCache) {
            hotApplied.add("performance.explosionBlockCache");
        }
        if (previous.performance.tntDetonationBudget != reloaded.performance.tntDetonationBudget) {
            hotApplied.add("performance.tntDetonationBudget");
        }
        if (previous.performance.explosionDropHashMerge != reloaded.performance.explosionDropHashMerge) {
            hotApplied.add("performance.explosionDropHashMerge");
        }
        if (previous.performance.explosionExposureClipCache != reloaded.performance.explosionExposureClipCache) {
            hotApplied.add("performance.explosionExposureClipCache");
        }
        if (previous.performance.explosionEntityCache != reloaded.performance.explosionEntityCache) {
            hotApplied.add("performance.explosionEntityCache");
        }
        if (previous.performance.entityHardCollisionCandidateIndex != reloaded.performance.entityHardCollisionCandidateIndex) {
            hotApplied.add("performance.entityHardCollisionCandidateIndex");
        }
        if (previous.performance.entitySectionChunkScan != reloaded.performance.entitySectionChunkScan) {
            hotApplied.add("performance.entitySectionChunkScan");
        }
        if (previous.performance.entityCollisionAbortPropagation != reloaded.performance.entityCollisionAbortPropagation) {
            hotApplied.add("performance.entityCollisionAbortPropagation");
        }
        if (previous.performance.pushableEntityConsumer != reloaded.performance.pushableEntityConsumer) {
            hotApplied.add("performance.pushableEntityConsumer");
        }
        if (previous.performance.entityMovementLazyColliders != reloaded.performance.entityMovementLazyColliders) {
            hotApplied.add("performance.entityMovementLazyColliders");
        }
        if (previous.performance.entityTrackerFastPath != reloaded.performance.entityTrackerFastPath) {
            hotApplied.add("performance.entityTrackerFastPath");
        }
        if (previous.performance.deepPassengerIteration != reloaded.performance.deepPassengerIteration) {
            hotApplied.add("performance.deepPassengerIteration");
        }
        if (previous.performance.entityTypeLookupFastPath != reloaded.performance.entityTypeLookupFastPath) {
            hotApplied.add("performance.entityTypeLookupFastPath");
        }
        if (previous.performance.randomTickPositionMask != reloaded.performance.randomTickPositionMask) {
            hotApplied.add("performance.randomTickPositionMask");
        }
        if (previous.performance.chunkGenerationTaskPlanCache != reloaded.performance.chunkGenerationTaskPlanCache) {
            hotApplied.add("performance.chunkGenerationTaskPlanCache");
        }
        if (previous.performance.chunkTaskDispatcherBatchLoop != reloaded.performance.chunkTaskDispatcherBatchLoop) {
            hotApplied.add("performance.chunkTaskDispatcherBatchLoop");
        }
        if (previous.performance.chunkStorageRegionScanFastPath != reloaded.performance.chunkStorageRegionScanFastPath) {
            hotApplied.add("performance.chunkStorageRegionScanFastPath");
        }
        io.fand.server.hooks.FandHooks.applyPerformanceConfig(reloaded.performance);
        if (previous.technical.zeroTickPlants != reloaded.technical.zeroTickPlants) {
            hotApplied.add("technical.zeroTickPlants");
        }
        if (previous.technical.oldHopperSuckInBehavior != reloaded.technical.oldHopperSuckInBehavior) {
            hotApplied.add("technical.oldHopperSuckInBehavior");
        }
        if (previous.technical.shearsInDispenserCanZeroAmount != reloaded.technical.shearsInDispenserCanZeroAmount) {
            hotApplied.add("technical.shearsInDispenserCanZeroAmount");
        }
        if (previous.technical.allowEntityPortalWithPassenger != reloaded.technical.allowEntityPortalWithPassenger) {
            hotApplied.add("technical.allowEntityPortalWithPassenger");
        }
        if (previous.technical.disableGatewayPortalEntityTicking != reloaded.technical.disableGatewayPortalEntityTicking) {
            hotApplied.add("technical.disableGatewayPortalEntityTicking");
        }
        if (previous.technical.disableLivingEntityAiStepAliveCheck != reloaded.technical.disableLivingEntityAiStepAliveCheck) {
            hotApplied.add("technical.disableLivingEntityAiStepAliveCheck");
        }
        if (previous.technical.spawnInvulnerableTime != reloaded.technical.spawnInvulnerableTime) {
            hotApplied.add("technical.spawnInvulnerableTime");
        }
        if (previous.technical.oldZombiePiglinDrop != reloaded.technical.oldZombiePiglinDrop) {
            hotApplied.add("technical.oldZombiePiglinDrop");
        }
        if (previous.technical.oldZombieReinforcement != reloaded.technical.oldZombieReinforcement) {
            hotApplied.add("technical.oldZombieReinforcement");
        }
        if (previous.technical.allowAnvilDestroyItemEntities != reloaded.technical.allowAnvilDestroyItemEntities) {
            hotApplied.add("technical.allowAnvilDestroyItemEntities");
        }
        if (previous.technical.disableItemDamageCheck != reloaded.technical.disableItemDamageCheck) {
            hotApplied.add("technical.disableItemDamageCheck");
        }
        if (previous.technical.keepLeashConnectWhenUseFirework != reloaded.technical.keepLeashConnectWhenUseFirework) {
            hotApplied.add("technical.keepLeashConnectWhenUseFirework");
        }
        if (previous.technical.tntWetExplosionNoItemDamage != reloaded.technical.tntWetExplosionNoItemDamage) {
            hotApplied.add("technical.tntWetExplosionNoItemDamage");
        }
        if (previous.technical.oldProjectileExplosionBehavior != reloaded.technical.oldProjectileExplosionBehavior) {
            hotApplied.add("technical.oldProjectileExplosionBehavior");
        }
        if (previous.technical.oldThrowableProjectileTickOrder != reloaded.technical.oldThrowableProjectileTickOrder) {
            hotApplied.add("technical.oldThrowableProjectileTickOrder");
        }
        if (previous.technical.oldMinecartMotionBehavior != reloaded.technical.oldMinecartMotionBehavior) {
            hotApplied.add("technical.oldMinecartMotionBehavior");
        }
        if (previous.technical.copperBulbOneGameTickDelay != reloaded.technical.copperBulbOneGameTickDelay) {
            hotApplied.add("technical.copperBulbOneGameTickDelay");
        }
        if (previous.technical.crafterOneGameTickDelay != reloaded.technical.crafterOneGameTickDelay) {
            hotApplied.add("technical.crafterOneGameTickDelay");
        }
        if (previous.technical.noTntPlaceUpdate != reloaded.technical.noTntPlaceUpdate) {
            hotApplied.add("technical.noTntPlaceUpdate");
        }
        if (previous.technical.allowPistonDuplication != reloaded.technical.allowPistonDuplication) {
            hotApplied.add("technical.allowPistonDuplication");
        }
        if (previous.technical.allowTntDuplication != reloaded.technical.allowTntDuplication) {
            hotApplied.add("technical.allowTntDuplication");
        }
        if (previous.technical.allowRailDuplication != reloaded.technical.allowRailDuplication) {
            hotApplied.add("technical.allowRailDuplication");
        }
        if (previous.technical.allowCarpetDuplication != reloaded.technical.allowCarpetDuplication) {
            hotApplied.add("technical.allowCarpetDuplication");
        }
        if (previous.technical.allowGravityBlockEndPortalDuplication != reloaded.technical.allowGravityBlockEndPortalDuplication) {
            hotApplied.add("technical.allowGravityBlockEndPortalDuplication");
        }
        if (previous.technical.redstoneIgnoreUpwardsUpdate != reloaded.technical.redstoneIgnoreUpwardsUpdate) {
            hotApplied.add("technical.redstoneIgnoreUpwardsUpdate");
        }
        if (previous.technical.movableBuddingAmethyst != reloaded.technical.movableBuddingAmethyst) {
            hotApplied.add("technical.movableBuddingAmethyst");
        }
        if (previous.technical.stringTripwireHookDuplicate != reloaded.technical.stringTripwireHookDuplicate) {
            hotApplied.add("technical.stringTripwireHookDuplicate");
        }
        if (!previous.technical.tripwireBehavior.equals(reloaded.technical.tripwireBehavior)) {
            hotApplied.add("technical.tripwireBehavior");
        }
        io.fand.server.hooks.FandHooks.applyTechnicalConfig(reloaded.technical);

        plugins.reconfigure(toPluginOptions(reloaded));
        current.set(reloaded);
        return new ConfigReloadResult(hotApplied, requiresRestart);
    }

    public static PluginRuntime.Options toPluginOptions(FandConfig config) {
        return new PluginRuntime.Options(
                config.plugins.continueOnLoadFailure,
                config.plugins.continueOnEnableFailure,
                config.plugins.logSummary
        );
    }
}
