package io.fand.api.hologram;

import io.fand.api.world.Location;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import net.kyori.adventure.text.Component;

/**
 * Runtime service for multi-line floating text.
 */
public interface HologramService {

    Collection<? extends Hologram> holograms();

    Optional<? extends Hologram> hologram(UUID id);

    CompletableFuture<? extends Hologram> create(
            Location location,
            List<? extends Component> lines,
            HologramOptions options);

    default CompletableFuture<? extends Hologram> create(Location location, List<? extends Component> lines) {
        return create(location, lines, HologramOptions.defaults());
    }

    default CompletableFuture<? extends Hologram> create(Location location, Component... lines) {
        Objects.requireNonNull(lines, "lines");
        return create(location, List.of(lines));
    }

    default CompletableFuture<? extends Hologram> create(
            Location location,
            HologramOptions options,
            Component... lines
    ) {
        Objects.requireNonNull(lines, "lines");
        return create(location, List.of(lines), options);
    }

    boolean remove(UUID id);

    static HologramService empty() {
        return new HologramService() {
            @Override
            public Collection<? extends Hologram> holograms() {
                return List.of();
            }

            @Override
            public Optional<? extends Hologram> hologram(UUID id) {
                Objects.requireNonNull(id, "id");
                return Optional.empty();
            }

            @Override
            public CompletableFuture<? extends Hologram> create(
                    Location location,
                    List<? extends Component> lines,
                    HologramOptions options
            ) {
                return CompletableFuture.failedFuture(new UnsupportedOperationException("Holograms are not supported"));
            }

            @Override
            public boolean remove(UUID id) {
                Objects.requireNonNull(id, "id");
                return false;
            }
        };
    }
}
