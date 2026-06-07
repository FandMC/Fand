package io.fand.api.entity;

import io.fand.api.component.DataComponentContainer;
import io.fand.api.world.Location;
import io.fand.api.world.World;
import java.util.UUID;
import net.kyori.adventure.key.Key;

/**
 * Base handle for any entity in a {@link World}.
 *
 * <p>Instances are thin handles backed by the underlying server entity. Reads
 * must happen on the server thread unless explicitly documented otherwise.
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

    /** Current location. May be slightly stale if read off the server thread. */
    Location location();

    /** World currently containing the entity. */
    World world();

    /**
     * Persistent Fand components attached to this entity UUID.
     *
     * <p>The returned container is live and backed by server save data. Component
     * reads and writes must happen on the server thread. Values survive entity
     * wrapper recreation and dimension changes as long as the same UUID is used.
     */
    DataComponentContainer components();
}
