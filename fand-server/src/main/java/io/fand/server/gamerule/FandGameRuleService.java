package io.fand.server.gamerule;

import io.fand.api.gamerule.GameRuleDefinition;
import io.fand.api.gamerule.GameRuleRegistration;
import io.fand.api.gamerule.GameRuleService;
import io.fand.api.world.World;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import net.kyori.adventure.key.Key;

public final class FandGameRuleService implements GameRuleService {

    private final Object lock = new Object();
    private final LinkedHashMap<Key, RegisteredRule> definitions = new LinkedHashMap<>();
    private final Map<Key, Map<Key, String>> valuesByWorld = new LinkedHashMap<>();
    private long sequence;

    @Override
    public Collection<GameRuleDefinition> definitions() {
        synchronized (lock) {
            return definitions.values().stream()
                    .map(RegisteredRule::definition)
                    .toList();
        }
    }

    @Override
    public Optional<GameRuleDefinition> definition(Key key) {
        Objects.requireNonNull(key, "key");
        synchronized (lock) {
            return Optional.ofNullable(definitions.get(key)).map(RegisteredRule::definition);
        }
    }

    @Override
    public GameRuleRegistration register(GameRuleDefinition definition) {
        Objects.requireNonNull(definition, "definition");
        synchronized (lock) {
            long token = ++sequence;
            definitions.put(definition.key(), new RegisteredRule(definition, token));
            return new RegisteredGameRule(this, definition.key(), token);
        }
    }

    @Override
    public Optional<String> value(World world, Key key) {
        Objects.requireNonNull(world, "world");
        Objects.requireNonNull(key, "key");
        synchronized (lock) {
            var definition = definitions.get(key);
            if (definition == null) {
                return Optional.empty();
            }
            return Optional.ofNullable(valuesByWorld.getOrDefault(world.key(), Map.of()).get(key))
                    .or(() -> Optional.of(definition.definition().defaultValue()));
        }
    }

    @Override
    public CompletableFuture<Boolean> setValue(World world, Key key, String value) {
        Objects.requireNonNull(world, "world");
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(value, "value");
        synchronized (lock) {
            var definition = definitions.get(key);
            if (definition == null || !definition.definition().validValue(value)) {
                return CompletableFuture.completedFuture(false);
            }
            valuesByWorld.computeIfAbsent(world.key(), ignored -> new LinkedHashMap<>()).put(key, value);
            return CompletableFuture.completedFuture(true);
        }
    }

    public Optional<String> value(Key world, Key key) {
        Objects.requireNonNull(world, "world");
        Objects.requireNonNull(key, "key");
        synchronized (lock) {
            var definition = definitions.get(key);
            if (definition == null) {
                return Optional.empty();
            }
            return Optional.ofNullable(valuesByWorld.getOrDefault(world, Map.of()).get(key))
                    .or(() -> Optional.of(definition.definition().defaultValue()));
        }
    }

    public boolean setValue(Key world, Key key, String value) {
        Objects.requireNonNull(world, "world");
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(value, "value");
        synchronized (lock) {
            var definition = definitions.get(key);
            if (definition == null || !definition.definition().validValue(value)) {
                return false;
            }
            valuesByWorld.computeIfAbsent(world, ignored -> new LinkedHashMap<>()).put(key, value);
            return true;
        }
    }

    private boolean active(Key key, long token) {
        synchronized (lock) {
            var current = definitions.get(key);
            return current != null && current.token() == token;
        }
    }

    private boolean unregister(Key key, long token) {
        synchronized (lock) {
            var current = definitions.get(key);
            if (current == null || current.token() != token) {
                return false;
            }
            definitions.remove(key);
            valuesByWorld.values().forEach(values -> values.remove(key));
            return true;
        }
    }

    private record RegisteredRule(GameRuleDefinition definition, long token) {
    }

    private static final class RegisteredGameRule implements GameRuleRegistration {

        private final FandGameRuleService owner;
        private final Key key;
        private final long token;
        private final AtomicBoolean active = new AtomicBoolean(true);

        private RegisteredGameRule(FandGameRuleService owner, Key key, long token) {
            this.owner = owner;
            this.key = key;
            this.token = token;
        }

        @Override
        public Key key() {
            return key;
        }

        @Override
        public boolean active() {
            return active.get() && owner.active(key, token);
        }

        @Override
        public void unregister() {
            if (active.compareAndSet(true, false)) {
                owner.unregister(key, token);
            }
        }
    }
}
