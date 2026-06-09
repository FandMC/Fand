package io.fand.api.world.generation;

/**
 * Selects whether a generated world keeps the template biome source or uses a
 * plugin supplied {@link BiomeProvider}.
 */
public enum VanillaBiomeSource {
    CUSTOM,
    TEMPLATE
}
