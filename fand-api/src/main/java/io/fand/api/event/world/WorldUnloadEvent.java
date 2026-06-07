package io.fand.api.event.world;

import io.fand.api.event.Event;
import io.fand.api.world.World;
import java.util.Objects;

/**
 * Fired on the server thread before a loaded world becomes unavailable.
 */
public final class WorldUnloadEvent implements Event {

    private final World world;

    public WorldUnloadEvent(World world) {
        this.world = Objects.requireNonNull(world, "world");
    }

    public World world() {
        return world;
    }
}
