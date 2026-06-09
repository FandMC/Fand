package io.fand.api.world.generation;

/**
 * Controls how much vanilla generation is kept before plugin callbacks run.
 */
public enum GenerationMode {
    EMPTY,
    CUSTOM,
    VANILLA,
    TEMPLATE
}
