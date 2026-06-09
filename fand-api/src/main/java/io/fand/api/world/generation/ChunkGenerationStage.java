package io.fand.api.world.generation;

/**
 * Vanilla chunk generation stages exposed for custom generation callbacks.
 */
public enum ChunkGenerationStage {
    EMPTY,
    STRUCTURE_STARTS,
    STRUCTURE_REFERENCES,
    BIOMES,
    NOISE,
    SURFACE,
    CARVERS,
    FEATURES,
    INITIALIZE_LIGHT,
    LIGHT,
    SPAWN,
    FULL
}
