package io.fand.server.plugin;

import io.fand.api.command.CommandRegistry;

/**
 * Test-only factory for constructing scoped registry instances without exposing
 * the package-private {@link PluginResourceTracker} to other source sets.
 */
public final class PluginRegistries {

    private PluginRegistries() {
    }

    public static PluginCommandRegistry testRegistry(CommandRegistry delegate, String namespace) {
        return new PluginCommandRegistry(delegate, new PluginResourceTracker(), namespace);
    }
}
