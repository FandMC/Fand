package io.fand.api.plugin;

import io.fand.api.service.ServiceRegistry;
import java.util.Collection;
import java.util.Optional;

/**
 * Lifecycle and lookup for all loaded plugins.
 */
public interface PluginManager {

    /** Snapshot of all currently loaded plugins. */
    Collection<Plugin> loaded();

    /** Descriptor snapshot for loaded plugins, in the same lifecycle order as {@link #loaded()}. */
    default Collection<PluginDescriptor> loadedDescriptors() {
        return java.util.List.of();
    }

    Optional<Plugin> byId(String id);

    /** Whether the plugin with the given id is currently enabled. */
    boolean isEnabled(String id);

    /** Cross-plugin Java service registry for provider discovery. */
    default ServiceRegistry services() {
        return ServiceRegistry.empty();
    }
}
