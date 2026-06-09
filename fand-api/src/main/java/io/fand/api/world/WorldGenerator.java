package io.fand.api.world;

/**
 * Callback used by custom generated worlds.
 *
 * <p>Generation may run on worker threads. Implementations must not touch live
 * world, entity, or player state from this callback.
 */
@FunctionalInterface
public interface WorldGenerator {

    void generate(GeneratedChunk chunk);
}
