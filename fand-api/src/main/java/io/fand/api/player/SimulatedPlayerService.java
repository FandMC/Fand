package io.fand.api.player;

import io.fand.api.entity.Player;
import io.fand.api.world.Location;
import java.util.Collection;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Creates real server-side simulated players.
 *
 * <p>A simulated player is backed by the server's normal player entity path:
 * it is visible to entity queries, collision, AI targeting, combat, item pickup,
 * scoreboard/team systems, and other code that works with online players. It is
 * not backed by a client socket.
 */
public interface SimulatedPlayerService {

    Collection<? extends Player> players();

    Optional<? extends Player> player(UUID uniqueId);

    CompletableFuture<? extends Player> create(PlayerProfile profile, Location location);

    CompletableFuture<? extends Player> create(PlayerProfile profile, Location location, SimulatedPlayerOptions options);

    default CompletableFuture<? extends Player> create(String name, Location location) {
        return create(offlineProfile(name), location);
    }

    default CompletableFuture<? extends Player> create(String name, Location location, SimulatedPlayerOptions options) {
        return create(offlineProfile(name), location, options);
    }

    CompletableFuture<Boolean> remove(UUID uniqueId);

    default CompletableFuture<Boolean> remove(Player player) {
        return remove(player.uniqueId());
    }

    boolean simulated(UUID uniqueId);

    default boolean simulated(Player player) {
        return simulated(player.uniqueId());
    }

    static SimulatedPlayerService empty() {
        return Empty.INSTANCE;
    }

    static PlayerProfile offlineProfile(String name) {
        java.util.Objects.requireNonNull(name, "name");
        return new PlayerProfile(
                UUID.nameUUIDFromBytes(("OfflinePlayer:" + name).getBytes(StandardCharsets.UTF_8)),
                name);
    }

    enum Empty implements SimulatedPlayerService {
        INSTANCE;

        @Override
        public Collection<? extends Player> players() {
            return java.util.List.of();
        }

        @Override
        public Optional<? extends Player> player(UUID uniqueId) {
            java.util.Objects.requireNonNull(uniqueId, "uniqueId");
            return Optional.empty();
        }

        @Override
        public CompletableFuture<? extends Player> create(PlayerProfile profile, Location location) {
            return CompletableFuture.failedFuture(new UnsupportedOperationException("Simulated players are not supported"));
        }

        @Override
        public CompletableFuture<? extends Player> create(
                PlayerProfile profile,
                Location location,
                SimulatedPlayerOptions options
        ) {
            return CompletableFuture.failedFuture(new UnsupportedOperationException("Simulated players are not supported"));
        }

        @Override
        public CompletableFuture<Boolean> remove(UUID uniqueId) {
            java.util.Objects.requireNonNull(uniqueId, "uniqueId");
            return CompletableFuture.completedFuture(false);
        }

        @Override
        public boolean simulated(UUID uniqueId) {
            java.util.Objects.requireNonNull(uniqueId, "uniqueId");
            return false;
        }
    }
}
