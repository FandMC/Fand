package io.fand.api.plugin;

import java.nio.file.Path;
import org.slf4j.Logger;

/**
 * Per-plugin runtime services. Provided by the server to {@link Plugin} callbacks.
 */
public interface PluginContext {

    PluginDescriptor descriptor();

    /** Logger pre-configured with the plugin's id as its name. */
    Logger logger();

    /** Writable data directory unique to this plugin. Created on first access. */
    Path dataDirectory();
}
