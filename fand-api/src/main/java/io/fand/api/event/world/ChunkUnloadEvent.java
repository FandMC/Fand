package io.fand.api.event.world;

import io.fand.api.event.Event;
import io.fand.api.world.World;
import java.util.Objects;

/**
 * Fired on the server thread when a full chunk becomes inaccessible.
 */
public final class ChunkUnloadEvent implements Event {

    private final World world;
    private final int chunkX;
    private final int chunkZ;

    public ChunkUnloadEvent(World world, int chunkX, int chunkZ) {
        this.world = Objects.requireNonNull(world, "world");
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
    }

    public World world() {
        return world;
    }

    public int chunkX() {
        return chunkX;
    }

    public int chunkZ() {
        return chunkZ;
    }
}
