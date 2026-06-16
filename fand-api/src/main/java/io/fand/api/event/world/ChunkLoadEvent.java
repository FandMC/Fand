package io.fand.api.event.world;

import io.fand.api.event.Event;
import io.fand.api.world.Chunk;
import io.fand.api.world.World;
import java.util.Objects;

/**
 * Fired on the server thread when a chunk becomes a full chunk.
 */
public final class ChunkLoadEvent implements Event {

    private final World world;
    private final Chunk chunk;
    private final int chunkX;
    private final int chunkZ;

    public ChunkLoadEvent(World world, int chunkX, int chunkZ) {
        this.world = Objects.requireNonNull(world, "world");
        this.chunk = world.chunkAt(chunkX, chunkZ);
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
    }

    public ChunkLoadEvent(Chunk chunk) {
        this.chunk = Objects.requireNonNull(chunk, "chunk");
        this.world = chunk.world();
        this.chunkX = chunk.x();
        this.chunkZ = chunk.z();
    }

    public World world() {
        return world;
    }

    public Chunk chunk() {
        return chunk;
    }

    public int chunkX() {
        return chunkX;
    }

    public int chunkZ() {
        return chunkZ;
    }
}
