package io.fand.api.entity;

import io.fand.api.component.DataComponentContainer;
import io.fand.api.world.Location;
import io.fand.api.world.Vector3;
import io.fand.api.world.World;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

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

    /** Vanilla entity type (e.g. {@code minecraft:zombie}). */
    EntityType type();

    /** Whether the entity is still in a loaded world and not removed. */
    boolean alive();

    /** Current location. May be slightly stale if read off the server thread. */
    Location location();

    /** World currently containing the entity. */
    World world();

    /** Current velocity. */
    Vector3 velocity();

    /** Sets velocity. Marshals to the server thread. */
    void setVelocity(Vector3 velocity);

    /** Teleports this entity. The future completes with {@code false} if it is no longer alive. */
    CompletableFuture<Boolean> teleport(Location destination);

    /** Removes this entity from the world. No-op for already removed entities. */
    void remove();

    /** Entity currently carrying this entity, if any. */
    Optional<? extends Entity> vehicle();

    /** Snapshot of entities riding this entity. */
    java.util.List<? extends Entity> passengers();

    /** Whether this entity is on the ground. */
    boolean onGround();

    /** Whether this entity is touching water. */
    boolean inWater();

    /** Whether this entity is touching lava. */
    boolean inLava();

    /** Remaining fire ticks. */
    int fireTicks();

    /** Sets remaining fire ticks. Marshals to the server thread. */
    void setFireTicks(int ticks);

    /** Entity age in ticks. */
    int ticksLived();

    /**
     * Persistent Fand components attached to this entity UUID.
     *
     * <p>The returned container is live and backed by server save data. Component
     * reads and writes must happen on the server thread. Values survive entity
     * wrapper recreation and dimension changes as long as the same UUID is used.
     */
    DataComponentContainer components();
}
