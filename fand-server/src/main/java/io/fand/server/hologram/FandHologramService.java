package io.fand.server.hologram;

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
import java.util.concurrent.ConcurrentHashMap;
import net.kyori.adventure.text.Component;

public final class FandHologramService implements HologramService, AutoCloseable {

    private final ConcurrentHashMap<UUID, FandHologram> holograms = new ConcurrentHashMap<>();

    @Override
    public Collection<? extends Hologram> holograms() {
        return List.copyOf(holograms.values());
    }

    @Override
    public Optional<? extends Hologram> hologram(UUID id) {
        Objects.requireNonNull(id, "id");
        return Optional.ofNullable(holograms.get(id)).filter(Hologram::active);
    }

    @Override
    public CompletableFuture<? extends Hologram> create(
            Location location,
            List<? extends Component> lines,
            HologramOptions options
    ) {
        var hologram = new FandHologram(this, location, lines, options);
        holograms.put(hologram.id(), hologram);
        return hologram.spawn().whenComplete((created, failure) -> {
            if (failure != null) {
                holograms.remove(hologram.id(), hologram);
            }
        });
    }

    @Override
    public boolean remove(UUID id) {
        Objects.requireNonNull(id, "id");
        var hologram = holograms.get(id);
        if (hologram == null) {
            return false;
        }
        hologram.close();
        return true;
    }

    void removeFromService(UUID id, FandHologram hologram) {
        holograms.remove(id, hologram);
    }

    @Override
    public void close() {
        for (var hologram : List.copyOf(holograms.values())) {
            hologram.close();
        }
        holograms.clear();
    }
}
