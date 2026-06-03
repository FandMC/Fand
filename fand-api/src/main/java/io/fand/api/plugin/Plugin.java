package io.fand.api.plugin;

import io.fand.api.Server;

/**
 * Entry point implemented by every Fand plugin.
 *
 * <p>The runtime instantiates the class declared in {@code fand-plugin.json} via
 * its no-arg constructor, then invokes {@link #onLoad}, {@link #onEnable}, and
 * {@link #onDisable} in order. Plugins should not retain references obtained
 * before {@link #onEnable}.
 */
public interface Plugin {

    /** Called once after the plugin is constructed but before any other plugins are enabled. */
    default void onLoad(PluginContext context) {}

    /** Called when the plugin is enabled. Register listeners, commands, and tasks here. */
    void onEnable(PluginContext context);

    /** Called when the plugin is being unloaded. Release resources promptly. */
    default void onDisable(PluginContext context) {}

    /** Convenience accessor for the running server. */
    default Server server() {
        return io.fand.api.Fand.server();
    }
}
