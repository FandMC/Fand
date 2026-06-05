package io.fand.api.entity;

import io.fand.api.world.Location;
import io.fand.api.world.World;
import java.util.UUID;
import net.kyori.adventure.key.Key;

/**
 * Base handle for any entity in a {@link World}.
 *
 * <p>Instances are thin handles backed by the underlying server entity. Reads
 * must happen on the main thread unless explicitly documented otherwise.
 * Equality is by {@link #uniqueId()}; a handle may become {@linkplain #alive()
 * non-alive} after the entity is removed (death, dimension change cleanup,
 * unload).
 */
public interface Entity {

    /** Persistent uuid. Stable across reloads and dimension changes. */
    UUID uniqueId();

    /**
     * Per-session numeric id used by the network protocol. Not stable across
     * sessions; do not persist.
     */
    int entityId();

    /** Vanilla entity type registry key (e.g. {@code minecraft:zombie}). */
    Key type();

    /** Whether the entity is still in a loaded world and not removed. */
    boolean alive();

    /** Current location. May be slightly stale if read off the main thread. */
    Location location();

    /** World currently containing the entity. */
    World world();
}
