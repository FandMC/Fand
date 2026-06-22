package io.fand.server.plugin;

import io.fand.api.entity.Player;
import io.fand.api.player.PlayerProfile;
import io.fand.api.player.SimulatedPlayerOptions;
import io.fand.api.player.SimulatedPlayerService;
import io.fand.api.world.Location;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class PluginSimulatedPlayerService implements SimulatedPlayerService {

    private final SimulatedPlayerService delegate;
    private final PluginResourceTracker tracker;

    public PluginSimulatedPlayerService(SimulatedPlayerService delegate, PluginResourceTracker tracker) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
        this.tracker = Objects.requireNonNull(tracker, "tracker");
    }

    @Override
    public Collection<? extends Player> players() {
        return delegate.players();
    }

    @Override
    public Optional<? extends Player> player(UUID uniqueId) {
        return delegate.player(uniqueId);
    }

    @Override
    public CompletableFuture<? extends Player> create(PlayerProfile profile, Location location) {
        return create(profile, location, SimulatedPlayerOptions.defaults());
    }

    @Override
    public CompletableFuture<? extends Player> create(
            PlayerProfile profile,
            Location location,
            SimulatedPlayerOptions options
    ) {
        return delegate.create(profile, location, options)
                .thenApply(player -> {
                    tracker.trackSimulatedPlayer(player.uniqueId(), delegate);
                    return player;
                });
    }

    @Override
    public CompletableFuture<Boolean> remove(UUID uniqueId) {
        tracker.releaseSimulatedPlayer(uniqueId);
        return delegate.remove(uniqueId);
    }

    @Override
    public boolean simulated(UUID uniqueId) {
        return delegate.simulated(uniqueId);
    }
}
