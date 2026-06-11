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
        if (previous.chunks.workerThreads != reloaded.chunks.workerThreads
                || previous.chunks.trackingDiffApplyBudget != reloaded.chunks.trackingDiffApplyBudget) {
            chunks.reconfigure(reloaded.chunks);
        }
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
        io.fand.server.hooks.FandHooks.applyPerformanceConfig(reloaded.performance);

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
