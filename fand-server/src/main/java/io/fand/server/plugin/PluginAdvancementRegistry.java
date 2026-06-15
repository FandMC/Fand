package io.fand.server.plugin;

import io.fand.api.advancement.AdvancementRegistry;
import io.fand.api.advancement.AdvancementRegistration;
import io.fand.api.advancement.AdvancementView;
import io.fand.api.advancement.CustomAdvancement;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import net.kyori.adventure.key.Key;

public final class PluginAdvancementRegistry implements AdvancementRegistry {

    private final AdvancementRegistry delegate;
    private final PluginResourceTracker tracker;
    private final String namespace;

    public PluginAdvancementRegistry(AdvancementRegistry delegate, PluginResourceTracker tracker, String namespace) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
        this.tracker = Objects.requireNonNull(tracker, "tracker");
        this.namespace = Objects.requireNonNull(namespace, "namespace");
    }

    @Override
    public Optional<AdvancementView> advancement(Key key) {
        return delegate.advancement(scopedKey(key)).filter(this::ownedByThisPlugin);
    }

    @Override
    public AdvancementRegistration register(CustomAdvancement advancement) {
        Objects.requireNonNull(advancement, "advancement");
        return tracker.track(delegate.register(new CustomAdvancement(
                scopedKey(advancement.key()),
                advancement.title(),
                advancement.description(),
                List.copyOf(advancement.criteria()))));
    }

    private Key scopedKey(Key key) {
        Objects.requireNonNull(key, "key");
        if (namespace.equals(key.namespace())) {
            return key;
        }
        return Key.key(namespace, key.value());
    }

    private boolean ownedByThisPlugin(AdvancementView view) {
        return namespace.equals(view.key().namespace());
    }
}
