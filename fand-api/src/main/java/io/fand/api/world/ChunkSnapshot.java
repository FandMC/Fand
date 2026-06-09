package io.fand.api.world;

/**
 * Lightweight immutable view of a chunk's current server-side state.
 */
public record ChunkSnapshot(
        World world,
        int chunkX,
        int chunkZ,
        boolean loaded,
        boolean forceLoaded,
        int entityCount
) {
}
