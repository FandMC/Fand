package io.fand.api.world;

import io.fand.api.world.generation.DecorationStep;
import io.fand.api.world.generation.GeneratorContext;
import java.util.List;

/**
 * Callback used by custom generated worlds.
 *
 * <p>Generation may run on worker threads. Implementations must not touch live
 * world, entity, or player state from this callback.
 */
@FunctionalInterface
public interface WorldGenerator {

    void generate(GeneratedChunk chunk);

    default void generate(GeneratedChunk chunk, GeneratorContext context) {
        generate(chunk);
    }

    default int baseHeight(int x, int z, HeightmapType type, GeneratorContext context, int fallback) {
        return fallback;
    }

    default void buildSurface(GeneratedChunk chunk, GeneratorContext context) {
    }

    default void carve(GeneratedChunk chunk, GeneratorContext context) {
    }

    default void decorate(GeneratedChunk chunk, DecorationStep step, GeneratorContext context) {
    }

    default void spawnOriginalMobs(GeneratorContext context) {
    }

    default void addDebugInfo(List<String> result, GeneratorContext context, int blockX, int blockY, int blockZ) {
    }
}
