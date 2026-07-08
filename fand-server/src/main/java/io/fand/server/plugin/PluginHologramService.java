package io.fand.server.plugin;

import io.fand.api.hologram.Hologram;
import io.fand.api.hologram.HologramOptions;
import io.fand.api.hologram.HologramService;
import io.fand.api.world.Location;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import net.kyori.adventure.text.Component;

public final class PluginHologramService implements HologramService {

    private final HologramService delegate;
    private final PluginResourceTracker tracker;

    public PluginHologramService(HologramService delegate, PluginResourceTracker tracker) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
        this.tracker = Objects.requireNonNull(tracker, "tracker");
    }

    @Override
    public Collection<? extends Hologram> holograms() {
        return tracker.holograms();
    }

    @Override
    public Optional<? extends Hologram> hologram(UUID id) {
        return tracker.hologram(id);
    }

    @Override
    public CompletableFuture<? extends Hologram> create(
            Location location,
            List<? extends Component> lines,
            HologramOptions options
    ) {
        return delegate.create(location, lines, options).thenApply(tracker::track);
    }

    @Override
    public boolean remove(UUID id) {
        var tracked = tracker.hologram(id);
        if (tracked.isEmpty()) {
            return false;
        }
        tracked.orElseThrow().close();
        return true;
    }
}
