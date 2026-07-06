package io.fand.api.event.world;

import io.fand.api.event.Event;
import io.fand.api.world.World;
import java.util.Objects;

/**
 * Fired on the server thread when a world is about to be flushed to disk.
 */
public record WorldSaveEvent(World world) implements Event {

    public WorldSaveEvent(World world) {
        this.world = Objects.requireNonNull(world, "world");
    }
}
