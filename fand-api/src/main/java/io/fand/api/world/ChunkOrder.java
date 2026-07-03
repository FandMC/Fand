package io.fand.api.world;

/**
 * Ordering policy for batch chunk operations.
 */
public enum ChunkOrder {

    /** Preserve the source iterable order after optional de-duplication. */
    SOURCE,

    /** Process chunks from the priority center outward. */
    NEAREST_FIRST,

    /** Process chunks from the priority center outward, preferring the facing direction within each ring. */
    FORWARD_FIRST
}
