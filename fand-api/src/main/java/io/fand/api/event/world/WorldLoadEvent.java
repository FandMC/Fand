package io.fand.api.event.world;

import io.fand.api.event.Event;
import io.fand.api.world.World;
import java.util.Objects;

/**
 * Fired on the server thread when a world becomes available to plugins.
 */
public record WorldLoadEvent(World world) implements Event {

    public WorldLoadEvent(World world) {
        this.world = Objects.requireNonNull(world, "world");
    }
}
