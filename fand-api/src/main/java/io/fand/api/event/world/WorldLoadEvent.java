package io.fand.api.event.world;

import io.fand.api.event.Event;
import io.fand.api.world.World;
import java.util.Objects;

/**
 * Fired on the server thread when a world becomes available to plugins.
 */
public final class WorldLoadEvent implements Event {

    private final World world;

    public WorldLoadEvent(World world) {
        this.world = Objects.requireNonNull(world, "world");
    }

    public World world() {
        return world;
    }
}
