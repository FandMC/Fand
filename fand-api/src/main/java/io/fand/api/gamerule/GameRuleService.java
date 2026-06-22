package io.fand.api.gamerule;

import io.fand.api.world.World;
import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import net.kyori.adventure.key.Key;

/** Registry and per-world value store for Fand custom game rules. */
public interface GameRuleService {

    static GameRuleService empty() {
        return Empty.INSTANCE;
    }

    Collection<GameRuleDefinition> definitions();

    Optional<GameRuleDefinition> definition(Key key);

    GameRuleRegistration register(GameRuleDefinition definition);

    Optional<String> value(World world, Key key);

    CompletableFuture<Boolean> setValue(World world, Key key, String value);

    enum Empty implements GameRuleService {
        INSTANCE;

        @Override
        public Collection<GameRuleDefinition> definitions() {
            return java.util.List.of();
        }

        @Override
        public Optional<GameRuleDefinition> definition(Key key) {
            return Optional.empty();
        }

        @Override
        public GameRuleRegistration register(GameRuleDefinition definition) {
            throw new UnsupportedOperationException("Custom game rules are not supported");
        }

        @Override
        public Optional<String> value(World world, Key key) {
            return Optional.empty();
        }

        @Override
        public CompletableFuture<Boolean> setValue(World world, Key key, String value) {
            return CompletableFuture.completedFuture(false);
        }
    }
}
