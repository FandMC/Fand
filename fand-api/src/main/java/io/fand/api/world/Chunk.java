package io.fand.api.world;

import io.fand.api.block.Block;
import io.fand.api.component.DataComponentKey;
import io.fand.api.entity.Entity;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;

/**
 * Live handle for a chunk coordinate pair in a world.
 *
 * <p>The handle is positional and lazy: it may represent a chunk that is not
 * currently loaded. Operations use the owning world's thread-safety semantics.
 */
public interface Chunk {

    World world();

    int x();

    int z();

    default ChunkPos pos() {
        return new ChunkPos(x(), z());
    }

    default int minBlockX() {
        return x() << 4;
    }

    default int minBlockZ() {
        return z() << 4;
    }

    default int maxBlockX() {
        return minBlockX() + 15;
    }

    default int maxBlockZ() {
        return minBlockZ() + 15;
    }

    default boolean loaded() {
        return world().chunkLoaded(x(), z());
    }

    default CompletableFuture<Boolean> load() {
        return world().loadChunk(x(), z());
    }

    default ChunkBatchOperation loadAround(int radius) {
        return world().loadChunksAround(pos(), radius);
    }

    default ChunkBatchOperation loadAround(int radius, ChunkBatchOptions options) {
        return world().loadChunksAround(pos(), radius, options);
    }

    default ChunkBatchOperation loadAroundPrioritized(int radius) {
        return loadAround(radius, ChunkBatchOptions.defaults().prioritize(pos()));
    }

    default CompletableFuture<Boolean> unload() {
        return world().unloadChunk(x(), z());
    }

    default boolean forceLoaded() {
        return world().chunkForceLoaded(x(), z());
    }

    default CompletableFuture<Boolean> setForceLoaded(boolean forceLoaded) {
        return world().setChunkForceLoaded(x(), z(), forceLoaded);
    }

    default ChunkBatchOperation setForceLoadedAround(int radius, boolean forceLoaded) {
        return world().setChunksForceLoadedAround(pos(), radius, forceLoaded);
    }

    default ChunkBatchOperation setForceLoadedAround(int radius, boolean forceLoaded, ChunkBatchOptions options) {
        return world().setChunksForceLoadedAround(pos(), radius, forceLoaded, options);
    }

    default int entityCount() {
        return world().entityCount(x(), z());
    }

    default Collection<? extends Entity> entities() {
        return world().entitiesInChunk(x(), z());
    }

    default Collection<? extends Block> blocksWith(DataComponentKey<?> key) {
        return world().blocksWith(key, x(), z());
    }

    default ChunkSnapshot snapshot() {
        return world().chunkSnapshot(x(), z());
    }

    default CompletableFuture<ChunkSnapshot> snapshotAsync() {
        return world().chunkSnapshotAsync(x(), z());
    }

    default Block blockAt(int localX, int y, int localZ) {
        if (localX < 0 || localX > 15) {
            throw new IllegalArgumentException("localX must be between 0 and 15, got " + localX);
        }
        if (localZ < 0 || localZ > 15) {
            throw new IllegalArgumentException("localZ must be between 0 and 15, got " + localZ);
        }
        return world().blockAt(minBlockX() + localX, y, minBlockZ() + localZ);
    }
}
