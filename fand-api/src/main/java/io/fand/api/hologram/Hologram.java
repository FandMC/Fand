package io.fand.api.hologram;

import io.fand.api.entity.TextDisplay;
import io.fand.api.world.Location;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import net.kyori.adventure.text.Component;

/**
 * Multi-line floating text backed by text display entities.
 */
public interface Hologram extends AutoCloseable {

    UUID id();

    Location location();

    HologramOptions options();

    List<Component> lines();

    List<? extends TextDisplay> displays();

    CompletableFuture<Void> teleport(Location location);

    CompletableFuture<Void> setLines(List<? extends Component> lines);

    default CompletableFuture<Void> setLine(int index, Component line) {
        var updated = new ArrayList<Component>(lines());
        updated.set(index, Objects.requireNonNull(line, "line"));
        return setLines(updated);
    }

    boolean active();

    @Override
    void close();
}
