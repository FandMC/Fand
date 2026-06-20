package io.fand.server.plugin;

import io.fand.api.entity.Player;
import io.fand.api.placeholder.PlaceholderProvider;
import io.fand.api.placeholder.PlaceholderRegistration;
import io.fand.api.placeholder.PlaceholderService;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

public final class PluginPlaceholderService implements PlaceholderService {

    private final PlaceholderService delegate;
    private final PluginResourceTracker tracker;
    private final String pluginId;

    public PluginPlaceholderService(PlaceholderService delegate, PluginResourceTracker tracker, String pluginId) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
        this.tracker = Objects.requireNonNull(tracker, "tracker");
        this.pluginId = Objects.requireNonNull(pluginId, "pluginId").toLowerCase(Locale.ROOT);
    }

    @Override
    public PlaceholderRegistration register(String namespace, PlaceholderProvider provider) {
        Objects.requireNonNull(provider, "provider");
        var normalized = Objects.requireNonNull(namespace, "namespace").trim().toLowerCase(Locale.ROOT);
        if (!normalized.equals(pluginId)) {
            throw new IllegalArgumentException("Plugin " + pluginId + " can only register placeholders in its own namespace");
        }
        return tracker.track(delegate.register(normalized, provider));
    }

    @Override
    public Optional<String> resolve(@Nullable Player viewer, String identifier) {
        return delegate.resolve(viewer, identifier);
    }

    @Override
    public String replace(@Nullable Player viewer, String input) {
        return delegate.replace(viewer, input);
    }
}
