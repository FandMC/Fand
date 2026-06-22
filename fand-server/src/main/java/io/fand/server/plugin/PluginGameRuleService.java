package io.fand.server.plugin;

import io.fand.api.gamerule.GameRuleDefinition;
import io.fand.api.gamerule.GameRuleRegistration;
import io.fand.api.gamerule.GameRuleService;
import io.fand.api.world.World;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import net.kyori.adventure.key.Key;

public final class PluginGameRuleService implements GameRuleService {

    private final GameRuleService delegate;
    private final PluginResourceTracker tracker;
    private final String namespace;

    public PluginGameRuleService(GameRuleService delegate, PluginResourceTracker tracker, String namespace) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
        this.tracker = Objects.requireNonNull(tracker, "tracker");
        this.namespace = Objects.requireNonNull(namespace, "namespace");
    }

    @Override
    public Collection<GameRuleDefinition> definitions() {
        return delegate.definitions().stream()
                .filter(definition -> namespace.equals(definition.key().namespace()))
                .toList();
    }

    @Override
    public Optional<GameRuleDefinition> definition(Key key) {
        return delegate.definition(scopedKey(key))
                .filter(definition -> namespace.equals(definition.key().namespace()));
    }

    @Override
    public GameRuleRegistration register(GameRuleDefinition definition) {
        Objects.requireNonNull(definition, "definition");
        return tracker.track(delegate.register(new GameRuleDefinition(
                scopedKey(definition.key()),
                definition.type(),
                definition.defaultValue(),
                definition.description())));
    }

    @Override
    public Optional<String> value(World world, Key key) {
        return delegate.value(world, scopedKey(key));
    }

    @Override
    public CompletableFuture<Boolean> setValue(World world, Key key, String value) {
        return delegate.setValue(world, scopedKey(key), value);
    }

    private Key scopedKey(Key key) {
        Objects.requireNonNull(key, "key");
        return Key.key(namespace, key.value());
    }
}
