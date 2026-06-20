package io.fand.api.entity;

import io.fand.api.component.DataComponentContainer;
import io.fand.api.world.Location;
import io.fand.api.world.Vector3;
import io.fand.api.world.World;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import net.kyori.adventure.text.Component;
import org.jspecify.annotations.Nullable;

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

    /** Custom display name, if one is set. */
    Optional<Component> customName();

    /** Sets or clears the custom display name. Marshals to the server thread. */
    void setCustomName(@Nullable Component name);

    /** Whether the custom display name is visible without looking at the entity. */
    boolean customNameVisible();

    /** Sets custom name visibility. Marshals to the server thread. */
    void setCustomNameVisible(boolean visible);

    /** Whether the entity has the glowing outline flag. */
    boolean glowing();

    /** Sets the glowing outline flag. Marshals to the server thread. */
    void setGlowing(boolean glowing);

    /** Whether the entity is silent. */
    boolean silent();

    /** Sets whether the entity is silent. Marshals to the server thread. */
    void setSilent(boolean silent);

    /** Whether vanilla gravity affects this entity. */
    boolean gravity();

    /** Sets whether vanilla gravity affects this entity. Marshals to the server thread. */
    void setGravity(boolean gravity);

    /** Whether regular damage sources are ignored. */
    boolean invulnerable();

    /** Sets invulnerability. Marshals to the server thread. */
    void setInvulnerable(boolean invulnerable);

    /** Snapshot of scoreboard/entity tags attached to this entity. */
    Set<String> scoreboardTags();

    /** Adds a scoreboard/entity tag. Marshals to the server thread. */
    void addScoreboardTag(String tag);

    /** Removes a scoreboard/entity tag. Marshals to the server thread. */
    void removeScoreboardTag(String tag);

    /** Current bounding-box width. */
    double width();

    /** Current bounding-box height. */
    double height();

    /** Teleports this entity. The future completes with {@code false} if it is no longer alive. */
    CompletableFuture<Boolean> teleport(Location destination);

    /** Removes this entity from the world. No-op for already removed entities. */
    void remove();

    /**
     * Entity currently carrying this entity, if any.
     *
     * <p>Reads live entity state; call from the server thread for a consistent
     * view. The wrapper lookup itself is thread-safe.
     */
    Optional<? extends Entity> vehicle();

    /**
     * Snapshot of entities riding this entity.
     *
     * <p>Reads live entity state; call from the server thread for a consistent
     * view. The wrapper lookup itself is thread-safe.
     */
    java.util.List<? extends Entity> passengers();

    /** Makes this entity ride {@code vehicle}. Completes with {@code false} when vanilla rejects it. */
    CompletableFuture<Boolean> mount(Entity vehicle);

    /** Makes {@code passenger} ride this entity. Completes with {@code false} when vanilla rejects it. */
    CompletableFuture<Boolean> addPassenger(Entity passenger);

    /** Dismounts {@code passenger} from this entity. Completes with {@code false} if it was not riding this entity. */
    CompletableFuture<Boolean> removePassenger(Entity passenger);

    /** Dismounts this entity from its current vehicle. */
    CompletableFuture<Boolean> dismount();

    /** Ejects all direct passengers from this entity. Marshals to the server thread. */
    void ejectPassengers();

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
