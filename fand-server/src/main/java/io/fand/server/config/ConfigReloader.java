package io.fand.server.config;

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

    public ConfigReloader(
            Path configPath,
            AtomicReference<FandConfig> current,
            PluginRuntime plugins,
            TaskScheduler scheduler
    ) {
        this.configPath = configPath;
        this.current = current;
        this.plugins = plugins;
        this.scheduler = scheduler;
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
        if (ProxyForwardingMode.fromConfig(previous.network.forwarding.mode)
                != ProxyForwardingMode.fromConfig(reloaded.network.forwarding.mode)) {
            requiresRestart.add("network.forwarding.mode");
        }
        if (!previous.network.forwarding.secret.equals(reloaded.network.forwarding.secret)) {
            requiresRestart.add("network.forwarding.secret");
        }

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
