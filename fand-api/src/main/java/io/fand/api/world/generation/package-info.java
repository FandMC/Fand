/**
 * World generation hooks: biomes, chunk stages, and generator settings.
 *
 * <p>Generation callbacks run on the chunk generation worker pool, not the
 * server thread. They must not block or touch live world/entity state.
 */
@org.jspecify.annotations.NullMarked
package io.fand.api.world.generation;
