package io.fand.server.plugin;

import io.fand.api.plugin.Plugin;
import io.fand.api.plugin.PluginManager;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public final class EmptyPluginManager implements PluginManager {

    @Override
    public Collection<Plugin> loaded() {
        return List.of();
    }

    @Override
    public Optional<Plugin> byId(String id) {
        return Optional.empty();
    }

    @Override
    public boolean isEnabled(String id) {
        return false;
    }
}
