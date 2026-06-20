package io.fand.server.plugin;

import io.fand.api.bossbar.BossBarHandle;
import io.fand.api.bossbar.BossBarRegistration;
import io.fand.api.bossbar.BossBarService;
import io.fand.api.entity.Player;
import java.time.Duration;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.key.Key;

public final class PluginBossBarService implements BossBarService {

    private final BossBarService delegate;
    private final PluginResourceTracker tracker;
    private final String namespace;

    public PluginBossBarService(BossBarService delegate, PluginResourceTracker tracker, String namespace) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
        this.tracker = Objects.requireNonNull(tracker, "tracker");
        this.namespace = Objects.requireNonNull(namespace, "namespace");
    }

    @Override
    public Collection<? extends BossBarRegistration> bars() {
        return delegate.bars().stream().filter(this::ownedByThisPlugin).toList();
    }

    @Override
    public Optional<? extends BossBarRegistration> bar(Key key) {
        return delegate.bar(scopedKey(key)).filter(this::ownedByThisPlugin);
    }

    @Override
    public BossBarRegistration register(Key key, BossBar bossBar) {
        return tracker.track(delegate.register(scopedKey(key), bossBar));
    }

    @Override
    public BossBarHandle send(Collection<? extends Player> viewers, BossBar bossBar, Duration duration) {
        return tracker.track(delegate.send(viewers, bossBar, duration));
    }

    @Override
    public boolean remove(Key key) {
        return delegate.remove(scopedKey(key));
    }

    private Key scopedKey(Key key) {
        Objects.requireNonNull(key, "key");
        if (namespace.equals(key.namespace())) {
            return key;
        }
        return Key.key(namespace, key.value());
    }

    private boolean ownedByThisPlugin(BossBarRegistration registration) {
        return namespace.equals(registration.key().namespace());
    }
}
